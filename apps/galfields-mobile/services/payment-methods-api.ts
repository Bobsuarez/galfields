import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';
import { appendImagePart, jsonPart } from '@/utils/multipart-form';

interface RemotePaymentMethod {
  paymentMethodId: number;
  methodName: string;
  active: boolean;
  imageUrl: string | null;
  createdAt: string;
}

export interface PaymentMethod {
  id: number;
  name: string;
  active: boolean;
  imageUrl: string | null;
}

/**
 * `imageUri` is only set when the user actively picked (or cleared) a photo
 * this session — it must never be the entity's existing remote `imageUrl`,
 * since that can't be attached as a multipart file part. Leaving it
 * `undefined` means "keep the current image" (see PaymentMethodController:
 * the image part is optional, an absent part leaves the stored image as-is).
 */
export interface PaymentMethodFormData {
  methodName: string;
  active: boolean;
  imageUri?: string | null;
}

const mapPaymentMethod = (r: RemotePaymentMethod): PaymentMethod => ({
  id: r.paymentMethodId,
  name: r.methodName,
  active: r.active,
  imageUrl: r.imageUrl,
});

async function checkResponse(response: Response, method: string, path: string): Promise<void> {
  if (response.ok) return;
  const text = await response.text().catch(() => '');
  console.error(`[payment-methods-api] ${method} ${path} -> ${response.status}`, text);
  throw new Error(parseApiErrorMessage(response.status, text));
}

async function send(path: string, method: 'POST' | 'PUT', data: PaymentMethodFormData): Promise<PaymentMethod> {
  const { imageUri, ...fields } = data;

  const formData = new FormData();
  formData.append('paymentMethod', (await jsonPart('paymentMethod', fields)) as unknown as Blob);
  if (imageUri) {
    appendImagePart(formData, 'image', imageUri);
  }

  const response = await fetch(`${apiBaseUrl()}${path}`, { method, body: formData });
  await checkResponse(response, method, path);

  const result: RemotePaymentMethod = await response.json();
  console.log(`[payment-methods-api] ${method} ${path} -> ${response.status}`);
  return mapPaymentMethod(result);
}

export const paymentMethodsApi = {
  list: async (): Promise<PaymentMethod[]> => {
    const path = '/api/payment-methods';
    const response = await fetch(`${apiBaseUrl()}${path}`);
    await checkResponse(response, 'GET', path);
    const result: RemotePaymentMethod[] = await response.json();
    return result.map(mapPaymentMethod);
  },
  create: (data: PaymentMethodFormData): Promise<PaymentMethod> => send('/api/payment-methods', 'POST', data),
  update: (id: number, data: PaymentMethodFormData): Promise<PaymentMethod> =>
    send(`/api/payment-methods/${id}`, 'PUT', data),
  remove: async (id: number): Promise<void> => {
    const path = `/api/payment-methods/${id}`;
    const response = await fetch(`${apiBaseUrl()}${path}`, { method: 'DELETE' });
    await checkResponse(response, 'DELETE', path);
  },
};
