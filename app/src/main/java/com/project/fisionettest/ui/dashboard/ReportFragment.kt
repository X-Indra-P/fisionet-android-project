package com.project.fisionettest.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.FragmentReportBinding
import com.project.fisionettest.utils.AppPreferences
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: AppPreferences
    private var isAdmin = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPreferences(requireContext())
        isAdmin = arguments?.getBoolean("isAdmin") ?: false

        setupSpinners()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnGeneratePdf.setOnClickListener {
            generateReport()
        }
    }

    private fun setupSpinners() {
        // Setup Clinic Spinner
        val clinics = if (isAdmin) {
            listOf("Cabang 1", "Cabang 2")
        } else {
            val userClinic = prefs.clinic ?: "Cabang 1"
            listOf(userClinic)
        }
        val clinicAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, clinics)
        clinicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerClinic.adapter = clinicAdapter
        if (!isAdmin) {
            binding.spinnerClinic.isEnabled = false // Lock clinic for therapists
        }

        // Setup Month Spinner
        val months = listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        
        // Select current month
        val currentMonthIdx = Calendar.getInstance().get(Calendar.MONTH)
        binding.spinnerMonth.setSelection(currentMonthIdx)

        // Setup Year Spinner
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 2 .. currentYear + 2).map { it.toString() }
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        
        // Select current year
        val yearPosition = years.indexOf(currentYear.toString())
        if (yearPosition != -1) {
            binding.spinnerYear.setSelection(yearPosition)
        }
    }

    private fun generateReport() {
        val selectedClinic = binding.spinnerClinic.selectedItem?.toString() ?: return
        val monthIdx = binding.spinnerMonth.selectedItemPosition + 1
        val selectedMonthName = binding.spinnerMonth.selectedItem.toString()
        val selectedYear = binding.spinnerYear.selectedItem?.toString()?.toIntOrNull() ?: return

        binding.btnGeneratePdf.isEnabled = false
        binding.progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val dateFrom = String.format("%04d-%02d-01", selectedYear, monthIdx)
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, monthIdx - 1)
                }
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dateTo = String.format("%04d-%02d-%02d", selectedYear, monthIdx, maxDay)

                // Query transactions for the clinic and date range, eager loading patients and packages
                val allTransactions = SupabaseClient.client
                    .from("transactions")
                    .select(columns = Columns.raw("*, patients(*), cabang_package(*, packages(*))")) {
                        filter {
                            val clinicId = com.project.fisionettest.utils.ClinicMapper.toId(selectedClinic) ?: 0
                            eq("id_cabang", clinicId)
                            gte("date", dateFrom)
                            lte("date", dateTo)
                        }
                    }
                    .decodeList<Transaction>()

                if (allTransactions.isEmpty()) {
                    Toast.makeText(requireContext(), "Tidak ada data transaksi pada periode ini", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Compute stats
                val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val lunasTrx = allTransactions.filter { it.payment_status in listOf("success", "paid", "settled") }
                val totalRevenue = lunasTrx.sumOf { it.total_amount }
                
                // Count unique patients who have transactions
                val uniquePatientCount = allTransactions.mapNotNull { it.patient_id }.distinct().size

                // Build HTML Report
                val htmlBuilder = StringBuilder()
                htmlBuilder.append("""
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { font-family: sans-serif; padding: 20px; color: #212121; }
                            .header { text-align: center; border-bottom: 3px double #1A237E; padding-bottom: 12dp; margin-bottom: 20px; }
                            .title { font-size: 26px; font-weight: bold; color: #1A237E; margin-bottom: 4px; }
                            .subtitle { font-size: 14px; color: #546E7A; text-transform: uppercase; letter-spacing: 1px; }
                            
                            .meta-table { width: 100%; margin-bottom: 25px; border-collapse: collapse; }
                            .meta-table td { padding: 6px; font-size: 13px; }
                            .meta-table td.label { font-weight: bold; width: 25%; color: #546E7A; }
                            
                            .summary-container { display: flex; justify-content: space-between; margin-bottom: 30px; gap: 15px; }
                            .summary-card { flex: 1; background: #F8F9FA; border: 1px solid #CFD8DC; border-radius: 8px; padding: 15px; text-align: center; }
                            .summary-card.highlight { background: #E8F0FE; border-color: #1A237E; }
                            .summary-val { font-size: 20px; font-weight: bold; color: #1A237E; margin-top: 5px; }
                            
                            .section-title { font-size: 16px; font-weight: bold; color: #1A237E; border-bottom: 2px solid #1A237E; padding-bottom: 6px; margin-bottom: 15px; }
                            
                            .trx-table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 12px; }
                            .trx-table th { background: #1A237E; color: white; text-align: left; padding: 10px; font-weight: bold; }
                            .trx-table td { border-bottom: 1px solid #E0E0E0; padding: 10px; vertical-align: middle; }
                            .trx-table tr:nth-child(even) { background-color: #F8F9FA; }
                            
                            .badge { padding: 4px 8px; border-radius: 4px; font-size: 10px; font-weight: bold; text-transform: uppercase; }
                            .badge-success { background: #E8F5E9; color: #2E7D32; }
                            .badge-pending { background: #FFF3E0; color: #E65100; }
                            .badge-failed { background: #FFEBEE; color: #C62828; }
                        </style>
                    </head>
                    <body>
                        <div class='header'>
                            <div class='title'>KLIK FISIO TERAPI</div>
                            <div class='subtitle'>Laporan Bulanan Aktivitas &amp; Pendapatan</div>
                        </div>
                        
                        <table class='meta-table'>
                            <tr>
                                <td class='label'>Cabang Klinik</td>
                                <td>: $selectedClinic</td>
                            </tr>
                            <tr>
                                <td class='label'>Periode Laporan</td>
                                <td>: $selectedMonthName $selectedYear</td>
                            </tr>
                            <tr>
                                <td class='label'>Waktu Cetak</td>
                                <td>: ${java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(java.util.Date())}</td>
                            </tr>
                        </table>
                        
                        <div class='section-title'>Rangkuman Statistik</div>
                        <div class='summary-container'>
                            <div class='summary-card'>
                                <div style='font-size: 11px; color: #546E7A;'>Jumlah Pasien Berobat</div>
                                <div class='summary-val'>$uniquePatientCount Pasien</div>
                            </div>
                            <div class='summary-card'>
                                <div style='font-size: 11px; color: #546E7A;'>Total Transaksi</div>
                                <div class='summary-val'>${allTransactions.size} Trx</div>
                            </div>
                            <div class='summary-card highlight'>
                                <div style='font-size: 11px; color: #1A237E; font-weight: bold;'>Total Pendapatan (Lunas)</div>
                                <div class='summary-val'>${format.format(totalRevenue)}</div>
                            </div>
                        </div>
                        
                        <div class='section-title'>Daftar Transaksi Pasien</div>
                        <table class='trx-table'>
                            <thead>
                                <tr>
                                    <th>Tanggal</th>
                                    <th>Nama Pasien</th>
                                    <th>Layanan / Paket</th>
                                    <th>Status Bayar</th>
                                    <th style='text-align: right;'>Jumlah</th>
                                </tr>
                            </thead>
                            <tbody>
                """.trimIndent())

                for (trx in allTransactions) {
                    val patientName = trx.patients?.name ?: "-"
                    val packageName = trx.cabang_package?.packages?.name ?: "Layanan Umum"
                    val statusClass = when (trx.payment_status?.lowercase()) {
                        "success", "paid", "settled" -> "badge-success"
                        "failed", "cancel" -> "badge-failed"
                        else -> "badge-pending"
                    }
                    val statusText = trx.payment_status ?: "pending"

                    htmlBuilder.append("""
                        <tr>
                            <td>${trx.date}</td>
                            <td>$patientName</td>
                            <td>$packageName</td>
                            <td><span class='badge $statusClass'>$statusText</span></td>
                            <td style='text-align: right;'>${format.format(trx.total_amount)}</td>
                        </tr>
                    """.trimIndent())
                }

                htmlBuilder.append("""
                            </tbody>
                        </table>
                    </body>
                    </html>
                """.trimIndent())

                printHtmlToPdf(htmlBuilder.toString(), "$selectedClinic - Laporan $selectedMonthName $selectedYear")

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal membuat laporan: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnGeneratePdf.isEnabled = true
                binding.progressLoading.visibility = View.GONE
            }
        }
    }

    private fun printHtmlToPdf(htmlContent: String, jobName: String) {
        val webView = WebView(requireContext())
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val printManager = requireContext().getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName.replace(" ", "_"))
                printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
