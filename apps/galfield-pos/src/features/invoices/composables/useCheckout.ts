import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import { type UnlistenFn } from '@tauri-apps/api/event'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import { usePrinterBus, type PrinterInvoicePayload } from '../../../composables/peripherals/usePrinterBus'
import { useCashDrawerBus } from '../../../composables/peripherals/useCashDrawerBus'
import { useAppConfig } from '../../../composables/useAppConfig'
import { useToast } from '../../../composables/useToast'
import { formatDate } from '../../../utils/currency'
import type { CartItem } from '../../../types'
import type InvoiceModal from '../components/InvoiceModal.vue'

interface CheckoutSale {
  customerName: string
  cartItems: CartItem[]
  subtotal: number
  discount: number
  total: number
  amountReceived: number
  /** Name, for display on the invoice/ticket (e.g. "Efectivo"). */
  paymentMethod: string
  /** Row id in `payment_method`, for the `sales.payment_method` foreign key. */
  paymentMethodId: number
}

/**
 * Orchestrates the "Facturar / Cobrar" flow: persists the sale, then either
 * prints it or archives it as a PDF (branching on `config.defaults.printReceipt`),
 * and opens the cash drawer.
 *
 * Printing and opening the drawer are independent peripheral actions — see
 * "Peripheral event model" in CLAUDE.md. Both are triggered right after the
 * sale is created and neither is awaited for its outcome, so a print failure
 * (paper out, printer off) can never stop the drawer from opening or hold up
 * the checkout flow. Their results are reported asynchronously as toasts via
 * the `peripheral-printer-*` / `peripheral-cash-drawer-*` event listeners
 * subscribed below, not by branching on this composable's own control flow.
 */
export function useCheckout(onComplete: () => void) {
  const { config }    = useAppConfig()
  const { show }      = useToast()
  const printerBus     = usePrinterBus()
  const cashDrawerBus  = useCashDrawerBus()

  const showModal       = ref(false)
  const isProcessing    = ref(false)
  const invoiceNumber   = ref('')
  const invoiceDate     = ref('')
  const modalRef        = ref<InstanceType<typeof InvoiceModal> | null>(null)

  let currentSale: CheckoutSale | null = null
  const unlisteners: UnlistenFn[] = []

  onMounted(async () => {
    unlisteners.push(
      await printerBus.onPrinterStatus(() => show('Factura enviada a la impresora', 'success')),
      await printerBus.onPrinterError(message => show(`Error de impresión: ${message}`, 'error')),
      await cashDrawerBus.onCashDrawerStatus(() => show('Caja registradora abierta', 'success')),
      await cashDrawerBus.onCashDrawerError(message => show(`No se pudo abrir la caja: ${message}`, 'error')),
    )
  })

  onUnmounted(() => {
    unlisteners.forEach(fn => fn())
    unlisteners.length = 0
  })

  function requestCheckout(sale: CheckoutSale) {
    if (sale.cartItems.length === 0) return
    currentSale         = sale
    invoiceNumber.value = 'Se generará al confirmar'
    invoiceDate.value   = formatDate(new Date())
    showModal.value     = true
  }

  function cancelCheckout() {
    if (isProcessing.value) return
    showModal.value = false
    currentSale      = null
  }

  function buildPrintPayload(sale: CheckoutSale, number: string, date: string, changeDue: number): PrinterInvoicePayload {
    return {
      storeName:     config.store.name,
      storeTaxId:    config.general.taxId,
      storeAddress:  config.store.address,
      storePhone:    config.store.phone,
      paperWidth:    config.peripherals.printerPaperWidth,
      invoiceNumber: number,
      date,
      seller:        config.defaults.seller,
      customer:      sale.customerName || config.defaults.customer,
      items: sale.cartItems.map(item => ({
        name:      item.product.productName,
        quantity:  item.quantity,
        lineTotal: item.unitPrice * item.quantity,
      })),
      subtotal:       sale.subtotal,
      discount:       sale.discount,
      total:          sale.total,
      paymentMethod:  sale.paymentMethod,
      amountReceived: sale.amountReceived,
      changeDue,
    }
  }

  async function archiveCurrentInvoiceAsPdf(number: string, createdAt: Date) {
    const element = modalRef.value?.getElement()
    if (!element) throw new Error('No se pudo capturar el documento de la factura')

    const canvas = await html2canvas(element, { scale: 2, backgroundColor: '#FDF9F0' })
    const pdf    = new jsPDF({ unit: 'px', format: [canvas.width, canvas.height] })
    pdf.addImage(canvas.toDataURL('image/png'), 'PNG', 0, 0, canvas.width, canvas.height)
    const pdfBytes = pdf.output('arraybuffer')

    await invoke('save_invoice_pdf', {
      baseFolder:     config.defaults.invoiceArchiveFolder,
      invoiceNumber:  number,
      pdfBytes:       Array.from(new Uint8Array(pdfBytes)),
      year:           String(createdAt.getFullYear()),
      month:          String(createdAt.getMonth() + 1).padStart(2, '0'),
      day:            String(createdAt.getDate()).padStart(2, '0'),
    })
  }

  async function confirmCheckout() {
    if (!currentSale || isProcessing.value) return
    const sale = currentSale

    if (!config.defaults.printReceipt && !config.defaults.invoiceArchiveFolder) {
      show('Configura una carpeta de archivo de facturas en Configuración → Por Defecto', 'error')
      return
    }

    isProcessing.value = true
    try {
      const result = await invoke<{ saleId: number; invoiceNumber: string; createdAt: string; changeDue: number }>(
        'create_sale',
        {
          paymentMethodId: sale.paymentMethodId,
          notes:          sale.customerName,
          items: sale.cartItems.map(item => ({
            productId: item.product.id,
            quantity:  item.quantity,
            unitPrice: item.unitPrice,
            subtotal:  item.unitPrice * item.quantity,
          })),
          subtotal:       sale.subtotal,
          discount:       sale.discount,
          total:          sale.total,
          amountReceived: sale.amountReceived,
          invoicePrefix:  config.sync.invoicePrefix,
        },
      )

      invoiceNumber.value = result.invoiceNumber
      invoiceDate.value   = formatDate(new Date(result.createdAt))
      await nextTick() // let InvoiceDocument re-render with the final invoice number before capture

      // Print (fire-and-forget peripheral event) or archive as PDF (awaited —
      // needs the modal's DOM still mounted). Either way, open the cash
      // drawer right after — independently, not gated by that outcome.
      if (config.defaults.printReceipt) {
        printerBus.triggerPrint(
          buildPrintPayload(sale, result.invoiceNumber, invoiceDate.value, result.changeDue),
          config.peripherals.printerPort,
        )
          .catch(() => { /* failure is reported via onPrinterError */ })
      } else {
        try {
          await archiveCurrentInvoiceAsPdf(result.invoiceNumber, new Date(result.createdAt))
        } catch (e) {
          console.error('[checkout] pdf archive failed:', e)
          show('No se pudo guardar el PDF de la factura', 'error')
        }
      }

      // Digital tender types (Nequi, cards, ...) never involve giving change,
      // so there's no reason to pop the drawer — only cash sales do.
      if (sale.paymentMethod === 'Efectivo') {
        cashDrawerBus.triggerCashDrawer(config.peripherals.printerPort)
          .catch(() => { /* failure is reported via onCashDrawerError */ })
      }

      show(`Factura ${result.invoiceNumber} generada con éxito`, 'success')
      showModal.value = false
      currentSale      = null
      onComplete()
    } catch (e) {
      console.error('[checkout] failed:', e)
      show('No se pudo completar la factura', 'error')
    } finally {
      isProcessing.value = false
    }
  }

  return {
    showModal,
    isProcessing,
    invoiceNumber,
    invoiceDate,
    modalRef,
    requestCheckout,
    cancelCheckout,
    confirmCheckout,
  }
}
