package co.com.galfields.pos_transactions.api.reportsaccess;

import co.com.galfields.pos_transactions.model.reportsaccess.ReportsAccessCode;
import co.com.galfields.pos_transactions.usecase.reportsaccess.ReportsAccessCodeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors backend/pos's ReportsAccessCodeController 1:1. */
@RestController
@RequestMapping("/api/reports-access-code")
@RequiredArgsConstructor
public class ReportsAccessCodeController {

    private final ReportsAccessCodeUseCase reportsAccessCodeUseCase;

    /** Called from mobile's Configuración → Acceso a Reportes when the
     * manager taps "Generar código". */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ReportsAccessCodeResponse generate() {
        ReportsAccessCode code = reportsAccessCodeUseCase.generate();
        return new ReportsAccessCodeResponse(code.getCode(), code.getGeneratedAt());
    }

    /** Called from the desktop POS every time a cashier enters the Reportes module. */
    @PostMapping("/validate")
    @Transactional(readOnly = true)
    public ReportsAccessCodeValidateResponse validate(@RequestBody @Valid ReportsAccessCodeValidateRequest request) {
        return new ReportsAccessCodeValidateResponse(reportsAccessCodeUseCase.validate(request.code()));
    }
}
