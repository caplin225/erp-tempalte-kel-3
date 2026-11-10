package id.nusantararasa.erp.customer;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.nusantararasa.erp.umum.DataTidakDitemukanException;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    static final String TIPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CustomerRepository customerRepository;
    private final CustomerExportService exportService;

    public CustomerController(CustomerRepository customerRepository, CustomerExportService exportService) {
        this.customerRepository = customerRepository;
        this.exportService = exportService;
    }

    @GetMapping
    public List<CustomerDto> daftar(@RequestParam(required = false) Segmen segmen) {
        List<Customer> hasil = segmen == null
                ? customerRepository.findAllByOrderByKodeAsc()
                : customerRepository.findBySegmenOrderByKodeAsc(segmen);
        return hasil.stream().map(CustomerDto::dari).toList();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> ekspor(@RequestParam(required = false) String kota,
            @RequestParam(required = false) String segmen) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(TIPE_XLSX))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customer.xlsx\"")
                .body(exportService.eksporExcel(kota, segmen));
    }

    @GetMapping("/{kode}")
    public CustomerDto detail(@PathVariable String kode) {
        return customerRepository.findByKode(kode)
                .map(CustomerDto::dari)
                .orElseThrow(() -> new DataTidakDitemukanException("Customer " + kode + " tidak ditemukan."));
    }
}
