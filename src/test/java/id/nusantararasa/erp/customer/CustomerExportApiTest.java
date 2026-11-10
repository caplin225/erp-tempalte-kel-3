package id.nusantararasa.erp.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerExportApiTest {

    private static final String TIPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    MockMvc mvc;

    private Sheet ekspor(String... param) throws Exception {
        var permintaan = get("/api/customers/export");
        for (int i = 0; i < param.length; i += 2) {
            permintaan.param(param[i], param[i + 1]);
        }
        byte[] isi = mvc.perform(permintaan)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TIPE_XLSX))
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook buku = new XSSFWorkbook(new ByteArrayInputStream(isi))) {
            return salin(buku.getSheetAt(0));
        }
    }

    /** Menyalin isi sheet ke struktur sederhana agar bisa dibaca setelah workbook ditutup. */
    private Sheet salin(Sheet asli) {
        XSSFWorkbook buku = new XSSFWorkbook();
        Sheet salinan = buku.createSheet("salinan");
        for (Row r : asli) {
            Row baru = salinan.createRow(r.getRowNum());
            for (int i = 0; i < r.getLastCellNum(); i++) {
                var sel = r.getCell(i);
                if (sel == null) {
                    continue;
                }
                switch (sel.getCellType()) {
                    case NUMERIC -> baru.createCell(i).setCellValue(sel.getNumericCellValue());
                    default -> baru.createCell(i).setCellValue(sel.getStringCellValue());
                }
            }
        }
        return salinan;
    }

    private List<String> kodeBaris(Sheet sheet) {
        List<String> kode = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            kode.add(sheet.getRow(i).getCell(0).getStringCellValue());
        }
        return kode;
    }

    @Test
    void eksporSemuaCustomerTanpaFilter() throws Exception {
        Sheet sheet = ekspor();
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Kode");
        assertThat(kodeBaris(sheet)).hasSize(50).startsWith("CUS-001").endsWith("CUS-050");
    }

    @Test
    void eksporFilterKotaJakartaUrutKode() throws Exception {
        assertThat(kodeBaris(ekspor("kota", "Jakarta")))
                .containsExactly("CUS-001", "CUS-009", "CUS-014", "CUS-021", "CUS-022", "CUS-031", "CUS-039");
    }

    @Test
    void eksporKotaTidakMembedakanHurufBesarKecil() throws Exception {
        assertThat(kodeBaris(ekspor("kota", "surabaya"))).hasSize(7);
    }

    @Test
    void eksporKotaHarusSamaPersis() throws Exception {
        Sheet sheet = ekspor("kota", "Jak");
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Kode");
        assertThat(kodeBaris(sheet)).isEmpty();
    }

    @Test
    void eksporKotaDigabungDenganSegmen() throws Exception {
        assertThat(kodeBaris(ekspor("kota", "Jakarta", "segmen", "TOKO")))
                .containsExactly("CUS-021", "CUS-022", "CUS-031", "CUS-039");
    }

    @Test
    void eksporSegmenTidakDikenalHanyaHeader() throws Exception {
        Sheet sheet = ekspor("kota", "Jakarta", "segmen", "XYZ");
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Kode");
        assertThat(kodeBaris(sheet)).isEmpty();
    }

    @Test
    void kolomSegmenBerisiLabelDanAngkaBerupaAngka() throws Exception {
        Sheet sheet = ekspor("kota", "Jakarta", "segmen", "TOKO");
        Row baris = sheet.getRow(1); // CUS-021
        assertThat(baris.getCell(3).getStringCellValue()).isEqualTo(Segmen.TOKO.getLabel());
        assertThat(baris.getCell(4).getNumericCellValue()).isEqualTo(14.0);
        assertThat(baris.getCell(5).getNumericCellValue()).isEqualTo(40000000.0);
    }
}
