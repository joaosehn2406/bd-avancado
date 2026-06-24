package com.jps.jps.shipment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final CassandraTemplate cassandraTemplate;

    @Value("${app.public-url:http://localhost:4200}")
    private String publicUrl;

    public ShipmentService(ShipmentRepository shipmentRepository, CassandraTemplate cassandraTemplate) {
        this.shipmentRepository = shipmentRepository;
        this.cassandraTemplate = cassandraTemplate;
    }

    public Shipment findByTrackingCode(String trackingCode) {
        return shipmentRepository.findById(trackingCode)
                .orElseThrow(() -> new ShipmentNotFoundException(trackingCode));
    }

    public ShipmentResponse getByTrackingCode(String trackingCode) {
        return ShipmentResponse.from(findByTrackingCode(trackingCode));
    }

    public ShipmentResponse create(ShipmentRequest request) {
        Shipment shipment = new Shipment(
                generateTrackingCode(),
                request.sender(),
                request.recipient(),
                request.origin(),
                request.destination(),
                Instant.now(),
                request.weightKg()
        );

        InsertOptions lwtOptions = InsertOptions.builder().withIfNotExists().build();
        var result = cassandraTemplate.insert(shipment, lwtOptions);
        if (!result.wasApplied()) {
            throw new DuplicateTrackingCodeException(shipment.trackingCode());
        }

        return ShipmentResponse.from(shipment, generateQrCode(shipment.trackingCode()));
    }

    public void delete(String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking code cannot be blank");
        }
        shipmentRepository.deleteById(trackingCode);
    }

    private String generateTrackingCode() {
        return "BR" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }

    private String generateQrCode(String trackingCode) {
        try {
            String url = publicUrl + "/rastreio/" + trackingCode;
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }
}
