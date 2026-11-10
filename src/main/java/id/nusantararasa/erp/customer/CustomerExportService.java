package id.nusantararasa.erp.customer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class CustomerExportService {

    static final String[] JUDUL_KOLOM = { "Kode", "Nama", "Kota", "Segmen", "Termin (hari)", "Limit kredit" };

    private final CustomerRepository customerRepository;

    public CustomerExportService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Membuat file xlsx berisi customer urut kode. Filter kota tidak membedakan huruf besar/kecil
     * tetapi harus sama persis. Segmen yang tidak dikenal menghasilkan file tanpa baris data.
     */
    public byte[] eksporExcel(String kota, String segmen) {
        List<Customer> hasil = customerRepository.findAllByOrderByKodeAsc().stream()
                .filter(c -> kota == null || (c.getKota() != null && c.getKota().equalsIgnoreCase(kota.trim())))
                .filter(c -> segmen == null || cocokSegmen(c.getSegmen(), segmen.trim()))
                .toList();
        return tulis(hasil);
    }

    private boolean cocokSegmen(Segmen segmenCustomer, String segmen) {
        return segmenCustomer != null && segmenCustomer.name().equalsIgnoreCase(segmen);
    }

    private byte[] tulis(List<Customer> daftar) {
        try (XSSFWorkbook buku = new XSSFWorkbook(); ByteArrayOutputStream keluaran = new ByteArrayOutputStream()) {
            Sheet lembar = buku.createSheet("Customer");
            Row judul = lembar.createRow(0);
            for (int i = 0; i < JUDUL_KOLOM.length; i++) {
                judul.createCell(i).setCellValue(JUDUL_KOLOM[i]);
            }
            int baris = 1;
            for (Customer c : daftar) {
                Row row = lembar.createRow(baris++);
                row.createCell(0).setCellValue(c.getKode());
                row.createCell(1).setCellValue(c.getNama());
                row.createCell(2).setCellValue(c.getKota());
                row.createCell(3).setCellValue(c.getSegmen() == null ? "" : c.getSegmen().getLabel());
                row.createCell(4).setCellValue(c.getTerminHari());
                // BigDecimal diubah ke angka hanya saat menulis sel
                row.createCell(5).setCellValue(c.getLimitKredit() == null ? 0 : c.getLimitKredit().doubleValue());
            }
            buku.write(keluaran);
            return keluaran.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
