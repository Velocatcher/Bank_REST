package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) { this.transferService = transferService; }

    @PostMapping
    public ResponseEntity<Long> transfer(@AuthenticationPrincipal UserDetails ud,
                                         @RequestBody @Valid TransferRequest req) {
        Long id = transferService.transfer(ud.getUsername(), req.fromCardId(), req.toCardId(), req.amount());
        return ResponseEntity.ok(id);
    }
}
