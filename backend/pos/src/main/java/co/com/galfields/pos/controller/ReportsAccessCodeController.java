package co.com.galfields.pos.controller;

import co.com.galfields.pos.dto.ReportsAccessCodeResponse;
import co.com.galfields.pos.dto.ReportsAccessCodeValidateRequest;
import co.com.galfields.pos.dto.ReportsAccessCodeValidateResponse;
import co.com.galfields.pos.service.ReportsAccessCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports-access-code")
@RequiredArgsConstructor
public class ReportsAccessCodeController {

    private final ReportsAccessCodeService reportsAccessCodeService;

    // Called from the mobile app's Configuración → Acceso a Reportes when
    // the manager taps "Generar código" — replaces whatever code was
    // active before (validate() always checks the most recently generated
    // one, see ReportsAccessCodeService).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportsAccessCodeResponse generate() {
        return reportsAccessCodeService.generate();
    }

    // Called from the desktop POS (apps/galfield-pos's reports_access.rs)
    // every time a cashier enters the Reportes module.
    @PostMapping("/validate")
    public ReportsAccessCodeValidateResponse validate(@RequestBody @Valid ReportsAccessCodeValidateRequest request) {
        return new ReportsAccessCodeValidateResponse(reportsAccessCodeService.validate(request.code()));
    }
}
