package com.ekote.controller;

import com.ekote.repositories.GunDetailsRepository;
import com.ekote.repositories.GunInfoRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/qrcode")
public class QRCodeController {

    @Autowired
    private final GunInfoRepository gunInfoRepository;

    public QRCodeController(GunInfoRepository gunDetailsRepository) {
        this.gunInfoRepository = gunDetailsRepository;
    }

    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQRCode(@RequestParam String gunId) {
        // ✅ Validate if gun exists
        boolean exists = gunInfoRepository.existsByUniqueIdentifier(gunId);
        if (!exists) {
            return ResponseEntity.badRequest().body(null);
        }

        // ✅ Generate QR Code content
        String qrCodeContent = "https://ekote-spring-production.up.railway.app/gunDetails/" + gunId;
        // You can change URL format based on your frontend or detail page

        try {
            byte[] qrImage = getQRCodeImage(qrCodeContent, 400, 400);
            return ResponseEntity.ok(qrImage);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] getQRCodeImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix matrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }
}
