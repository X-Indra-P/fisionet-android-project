package com.project.fisionettest.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.project.fisionettest.data.SupabaseClient
import com.project.fisionettest.data.model.Transaction
import com.project.fisionettest.databinding.FragmentAdminStatsBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

import androidx.navigation.fragment.findNavController
import com.project.fisionettest.R

class AdminStatsFragment : Fragment() {

    private var _binding: FragmentAdminStatsBinding? = null
    private val binding get() = _binding!!

    private var transactionsList: List<Transaction> = emptyList()
    private var monthName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats()

        binding.btnRefresh.setOnClickListener { loadStats() }
        binding.btnExportPdf.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isAdmin", true)
            }
            findNavController().navigate(R.id.action_adminDashboard_to_report, bundle)
        }

        binding.layoutCabang1.setOnClickListener {
            showBranchDetailDialog("Cabang 1")
        }
        binding.layoutCabang2.setOnClickListener {
            showBranchDetailDialog("Cabang 2")
        }
    }

    private fun exportStatsToPdf() {
        val webView = android.webkit.WebView(requireContext())
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        
        // Calculate variables
        val cabang1 = transactionsList.filter { it.id_cabang == 1 }
        val cabang2 = transactionsList.filter { it.id_cabang == 2 }
        val rev1 = cabang1.filter { it.payment_status in listOf("success", "paid", "settled") }.sumOf { it.total_amount }
        val rev2 = cabang2.filter { it.payment_status in listOf("success", "paid", "settled") }.sumOf { it.total_amount }
        val totalRev = rev1 + rev2

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; padding: 20px; color: #212121; }
                    .header { text-align: center; border-bottom: 2px solid #1A237E; padding-bottom: 10px; margin-bottom: 20px; }
                    .title { font-size: 24px; font-weight: bold; color: #1A237E; }
                    .subtitle { font-size: 14px; color: #757575; }
                    .summary-card { background: #1A237E; color: white; border-radius: 8px; padding: 20px; margin-bottom: 30px; text-align: center; }
                    .summary-amount { font-size: 28px; font-weight: bold; margin: 8px 0; }
                    .branch-table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
                    .branch-table th { background: #F5F5F5; border-bottom: 2px solid #E0E0E0; text-align: left; padding: 12px; }
                    .branch-table td { border-bottom: 1px solid #E0E0E0; padding: 12px; }
                    .total-row { font-weight: bold; }
                </style>
            </head>
            <body>
                <div class='header'>
                    <div class='title'>KLIK FISIO TERAPI</div>
                    <div class='subtitle'>Laporan Statistik & Pendapatan Bulanan</div>
                    <div style='margin-top: 5px; font-size: 12px;'>Periode: $monthName</div>
                </div>

                <div class='summary-card'>
                    <div style='font-size: 12px; text-transform: uppercase;'>Total Pendapatan Terkonfirmasi</div>
                    <div class='summary-amount'>${format.format(totalRev)}</div>
                    <div>Total: ${transactionsList.size} Transaksi</div>
                </div>

                <table class='branch-table'>
                    <thead>
                        <tr>
                            <th>Cabang</th>
                            <th>Total Transaksi</th>
                            <th>Total Pendapatan (Lunas)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Cabang 1</td>
                            <td>${cabang1.size}</td>
                            <td>${format.format(rev1)}</td>
                        </tr>
                        <tr>
                            <td>Cabang 2</td>
                            <td>${cabang2.size}</td>
                            <td>${format.format(rev2)}</td>
                        </tr>
                        <tr class='total-row'>
                            <td>Total Gabungan</td>
                            <td>${transactionsList.size}</td>
                            <td>${format.format(totalRev)}</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent())

        webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "utf-8", null)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                val printManager = requireContext().getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Laporan_Statistik_${monthName.replace(" ", "_")}")
                printManager.print("KlikFisio_Laporan_Statistik", printAdapter, android.print.PrintAttributes.Builder().build())
            }
        }
    }

    private fun loadStats() {
        binding.btnRefresh.isEnabled = false
        binding.progressStats.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Ambil semua transaksi bulan ini
                val calendar = Calendar.getInstance()
                val year  = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val dateFrom = String.format("%04d-%02d-01", year, month)
                val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dateTo   = String.format("%04d-%02d-%02d", year, month, maxDay)

                val allTransactions = SupabaseClient.client
                    .from("transactions")
                    .select(columns = Columns.list("id", "total_amount", "cabang", "payment_status", "date")) {
                        filter {
                            gte("date", dateFrom)
                            lte("date", dateTo)
                        }
                    }
                    .decodeList<Transaction>()

                val activeBinding = _binding ?: return@launch
                transactionsList = allTransactions

                // Hitung per cabang
                val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val cabang1 = allTransactions.filter { it.id_cabang == 1 }
                val cabang2 = allTransactions.filter { it.id_cabang == 2 }

                val rev1 = cabang1.filter { it.payment_status in listOf("success", "paid", "settled") }
                    .sumOf { it.total_amount }
                val rev2 = cabang2.filter { it.payment_status in listOf("success", "paid", "settled") }
                    .sumOf { it.total_amount }
                val totalRev = rev1 + rev2

                val mName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendar.time)
                monthName = mName

                // ── Update UI ──────────────────────────────────────────────
                activeBinding.tvPeriode.text = "Periode: $mName"

                // Cabang 1
                activeBinding.tvRev1.text      = format.format(rev1)
                activeBinding.tvTrxCount1.text = "${cabang1.size} transaksi"

                // Cabang 2
                activeBinding.tvRev2.text      = format.format(rev2)
                activeBinding.tvTrxCount2.text = "${cabang2.size} transaksi"

                // Total
                activeBinding.tvTotalRev.text   = format.format(totalRev)
                activeBinding.tvTotalTrx.text   = "${allTransactions.size} transaksi"

                // Progress bar perbandingan
                if (totalRev > 0) {
                    val pct1 = ((rev1 / totalRev) * 100).toInt()
                    activeBinding.progressCabang1.progress = pct1
                    activeBinding.progressCabang2.progress = 100 - pct1
                    activeBinding.tvPct1.text = "$pct1%"
                    activeBinding.tvPct2.text = "${100 - pct1}%"
                } else {
                    activeBinding.progressCabang1.progress = 50
                    activeBinding.progressCabang2.progress = 50
                    activeBinding.tvPct1.text = "0%"
                    activeBinding.tvPct2.text = "0%"
                }

            } catch (e: java.util.concurrent.CancellationException) {
                // Ignore
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Gagal memuat statistik: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _binding?.let { b ->
                    b.progressStats.visibility = View.GONE
                    b.btnRefresh.isEnabled = true
                }
            }
        }
    }

    private fun showBranchDetailDialog(branchName: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_branch_details, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_title)
        val pbLoading = dialogView.findViewById<android.widget.ProgressBar>(R.id.pb_dialog_loading)
        val rvStats = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_monthly_stats)
        val btnClose = dialogView.findViewById<android.view.View>(R.id.btn_close_dialog)

        tvTitle.text = "Detail Pendapatan - $branchName"
        btnClose.setOnClickListener { dialog.dismiss() }

        rvStats.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch all transactions for this branch
                val transactions = SupabaseClient.client
                    .from("transactions")
                    .select(columns = Columns.list("total_amount", "payment_status", "date")) {
                        filter {
                            eq("cabang", branchName)
                        }
                    }
                    .decodeList<Transaction>()

                // Group by month (yyyy-MM)
                val validTrx = transactions.filter { it.date.length >= 7 }
                val groups = validTrx.groupBy { it.date.substring(0, 7) }

                val statItems = groups.map { (monthKey, list) ->
                    val successTrx = list.filter { it.payment_status in listOf("success", "paid", "settled") }
                    val revenue = successTrx.sumOf { it.total_amount }
                    val monthName = getReadableMonth(monthKey)
                    MonthlyStat(monthKey, monthName, revenue, list.size)
                }.sortedByDescending { it.monthKey }

                if (isAdded) {
                    pbLoading.visibility = View.GONE
                    rvStats.visibility = View.VISIBLE
                    rvStats.adapter = MonthlyStatsAdapter(statItems)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Gagal memuat detail cabang: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getReadableMonth(key: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
            val date = parser.parse(key)
            if (date != null) formatter.format(date) else key
        } catch (e: Exception) {
            key
        }
    }

    data class MonthlyStat(val monthKey: String, val monthName: String, val revenue: Double, val trxCount: Int)

    private class MonthlyStatsAdapter(private val items: List<MonthlyStat>) : 
        androidx.recyclerview.widget.RecyclerView.Adapter<MonthlyStatsAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvMonth: android.widget.TextView = view.findViewById(R.id.tv_stat_month)
            val tvRevenue: android.widget.TextView = view.findViewById(R.id.tv_stat_revenue)
            val tvTrxCount: android.widget.TextView = view.findViewById(R.id.tv_stat_trx_count)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_branch_monthly_stat, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvMonth.text = item.monthName
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            holder.tvRevenue.text = format.format(item.revenue)
            holder.tvTrxCount.text = "${item.trxCount} transaksi"
        }

        override fun getItemCount(): Int = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
