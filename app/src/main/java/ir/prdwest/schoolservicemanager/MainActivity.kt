package ir.prdwest.schoolservicemanager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var db: DatabaseHelper
    private lateinit var root: LinearLayout
    private var startYear = 0
    private val months = PersianCalendar.months
    private val blue = Color.rgb(20, 108, 205)
    private val blue2 = Color.rgb(35, 135, 225)
    private val navy = Color.rgb(38, 55, 78)
    private val bgColor = Color.rgb(246, 249, 253)
    private val green = Color.rgb(25, 165, 93)
    private val red = Color.rgb(239, 73, 88)
    private val purple = Color.rgb(105, 72, 218)
    private val orange = Color.rgb(245, 143, 28)
    private val teal = Color.rgb(14, 174, 177)
    private val line = Color.rgb(222, 229, 238)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = blue
        window.navigationBarColor = Color.WHITE
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        db = DatabaseHelper(this)
        val t = PersianCalendar.today()
        startYear = if (t.m <= 6) t.y - 1 else t.y
        showDashboard()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, radius: Int = 16, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            stroke?.let { setStroke(dp(1), it) }
        }

    private fun text(s: String, size: Float = 14f, color: Int = navy, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }

    private fun base(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(bgColor)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun body(): ScrollView = ScrollView(this).apply {
        isFillViewport = true
        setPadding(dp(10), dp(10), dp(10), dp(16))
    }

    private fun header(title: String, subtitle: String? = null): View {
        val h = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = shape(blue, 0)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        h.addView(text("☰", 25f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showMenu() }
        }, LinearLayout.LayoutParams(dp(48), dp(52)))
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        titles.addView(text(title, 19f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        subtitle?.let { titles.addView(text(it, 11f, Color.rgb(225, 240, 255)).apply { gravity = Gravity.CENTER }) }
        h.addView(titles, LinearLayout.LayoutParams(0, -1, 1f))
        h.addView(text("♟", 22f, Color.WHITE).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(48), dp(52)))
        return h
    }

    private fun showDashboard() {
        val r = base()
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(blue2, blue))
            setPadding(dp(16), dp(18), dp(16), dp(18))
        }
        hero.addView(text("🏫", 44f, Color.WHITE).apply { gravity = Gravity.CENTER })
        hero.addView(text("مدیریت سرویس مدرسه", 24f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        hero.addView(text("با هم برای آینده بهتر", 13f, Color.WHITE).apply { gravity = Gravity.CENTER })
        r.addView(hero)

        val scroll = body()
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        filters.addView(spinnerBox("سال تحصیلی", startYear.toString() + "-" + (startYear + 1)), LinearLayout.LayoutParams(0, dp(70), 1f).apply { setMargins(0, 0, dp(5), 0) })
        val nowMonth = months[(PersianCalendar.today().m - 1).coerceIn(0, 11)]
        filters.addView(spinnerBox("ماه", nowMonth), LinearLayout.LayoutParams(0, dp(70), 1f).apply { setMargins(dp(5), 0, 0, 0) })
        c.addView(filters)

        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val st = db.students()
        val ay = academicYear()
        val pairs = PersianCalendar.academicMonths(startYear)
        var paid = 0
        var received = 0L
        st.forEach { s -> pairs.forEach { p -> if (db.payment(s.id, ay, p.first, p.second)?.paid == true) { paid++; received += s.fee } } }
        val total = st.size * 12
        statCard(stats, "✓", "پرداخت شده", paid.toString(), green)
        statCard(stats, "+", "پرداخت نشده", (total - paid).coerceAtLeast(0).toString(), red)
        statCard(stats, "₿", "دریافت شده", money(received), teal, "تومان")
        statCard(stats, "▣", "مانده", money((st.sumOf { it.fee } * 12 - received).coerceAtLeast(0)), purple, "تومان")
        c.addView(stats, LinearLayout.LayoutParams(-1, dp(112)).apply { setMargins(0, dp(10), 0, 0) })

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bigAction(row1, "🎓", "دانش‌آموزان", "مشاهده و مدیریت دانش‌آموزان", blue) { showStudents() }
        bigAction(row1, "＋", "افزودن دانش‌آموز", "ثبت دانش‌آموز جدید", green) { addStudent() }
        c.addView(row1, LinearLayout.LayoutParams(-1, dp(108)).apply { setMargins(0, dp(10), 0, 0) })

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bigAction(row2, "☁", "پشتیبان‌گیری", "ذخیره اطلاعات برنامه", orange) { backupExport() }
        bigAction(row2, "↥", "بازیابی", "بارگذاری اطلاعات", purple) { restoreFile() }
        c.addView(row2, LinearLayout.LayoutParams(-1, dp(108)).apply { setMargins(0, dp(8), 0, 0) })

        c.addView(actionCard("▥", "گزارش ماهانه", "مشاهده گزارش پرداخت‌ها", teal) { monthlyReport() }, LinearLayout.LayoutParams(-1, dp(90)).apply { setMargins(0, dp(8), 0, 0) })

        val tip = text("با نظم و برنامه‌ریزی، آموزش بهتر را تجربه کنیم.", 12f, Color.rgb(90, 110, 130))
        tip.gravity = Gravity.CENTER
        tip.background = shape(Color.rgb(232, 244, 255), 14)
        c.addView(tip, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(10), 0, dp(6)) })
        c.addView(bottomNav(0))
        scroll.addView(c)
        r.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(r)
    }

    private fun spinnerBox(title: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.WHITE, 12, line)
            setPadding(dp(6), dp(4), dp(6), dp(4))
            addView(text(title, 11f, Color.DKGRAY, true))
            addView(text(value + "  ⌄", 13f, navy, true))
        }
    }

    private fun statCard(parent: LinearLayout, icon: String, title: String, value: String, color: Int, unit: String = "") {
        val b = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(Color.WHITE, 13, line)
        }
        b.addView(text(icon, 23f, color, true).apply { gravity = Gravity.CENTER })
        b.addView(text(title, 10f, Color.DKGRAY, true).apply { gravity = Gravity.CENTER })
        b.addView(text(value, 14f, color, true).apply { gravity = Gravity.CENTER })
        if (unit.isNotEmpty()) b.addView(text(unit, 9f, Color.DKGRAY).apply { gravity = Gravity.CENTER })
        parent.addView(b, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun bigAction(parent: LinearLayout, icon: String, title: String, sub: String, color: Int, click: () -> Unit) {
        parent.addView(actionCard(icon, title, sub, color, click), LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun actionCard(icon: String, title: String, sub: String, color: Int, click: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape(color, 14)
            setPadding(dp(8), dp(7), dp(8), dp(7))
            setOnClickListener { click() }
            addView(text(icon, 28f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
            addView(text(title, 15f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
            addView(text(sub, 10f, Color.WHITE).apply { gravity = Gravity.CENTER })
        }
    }

    private fun showStudents() {
        val r = base()
        r.addView(header("دانش‌آموزان"))
        val scroll = body()
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        filters.addView(spinnerBox("سال تحصیلی", startYear.toString() + "-" + (startYear + 1)), LinearLayout.LayoutParams(0, dp(68), 1f).apply { setMargins(0, 0, dp(4), 0) })
        filters.addView(spinnerBox("ماه", months[(PersianCalendar.today().m - 1).coerceIn(0, 11)]), LinearLayout.LayoutParams(0, dp(68), 1f).apply { setMargins(dp(4), 0, 0, 0) })
        c.addView(filters)

        c.addView(text("＋  افزودن دانش‌آموز", 15f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = shape(green, 12)
            setOnClickListener { addStudent() }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(8), 0, dp(8)) })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.WHITE, 14, line)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        card.addView(text("جدول دانش‌آموزان", 17f, navy, true))
        val table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        table.addView(tableHeader())
        val ay = academicYear()
        val pairs = PersianCalendar.academicMonths(startYear)
        val students = db.students()
        students.forEachIndexed { i, s -> table.addView(studentRow(i + 1, s, ay, pairs)) }
        if (students.isEmpty()) table.addView(text("دانش‌آموزی ثبت نشده است.", 15f, Color.DKGRAY).apply { gravity = Gravity.CENTER })
        card.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = true
            addView(table)
        }, LinearLayout.LayoutParams(-1, dp(430)))
        c.addView(card)
        c.addView(bottomNav(1))
        scroll.addView(c)
        r.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(r)
    }

    private fun tableHeader(): View {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cell(r, "ردیف", 55, blue, true)
        cell(r, "نام دانش‌آموز", 145, blue, true)
        cell(r, "شهریه", 100, blue, true)
        months.forEach { cell(r, it, 70, blue, true) }
        return r
    }

    private fun cell(p: LinearLayout, s: String, w: Int, color: Int, bold: Boolean) {
        p.addView(text(s, 11f, if (color == blue) Color.WHITE else navy, bold).apply {
            gravity = Gravity.CENTER
            background = shape(color, 5)
        }, LinearLayout.LayoutParams(dp(w), dp(45)).apply { setMargins(dp(1), dp(1), dp(1), dp(1)) })
    }

    private fun studentRow(n: Int, s: Student, ay: String, pairs: List<Pair<Int, Int>>): View {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(if (n % 2 == 0) Color.rgb(249, 251, 254) else Color.WHITE)
        }
        cellLight(r, n.toString(), 55)
        cellLight(r, s.name, 145, true)
        cellLight(r, money(s.fee), 100)
        pairs.forEachIndexed { mi, p ->
            val pay = db.payment(s.id, ay, p.first, p.second)
            val ok = pay?.paid == true
            val v = text(if (ok) "✓" else "−", 21f, if (ok) green else red, true).apply {
                gravity = Gravity.CENTER
                background = shape(if (ok) Color.rgb(231, 249, 238) else Color.rgb(255, 241, 243), 7)
                setOnClickListener {
                    db.setPaid(s.id, ay, p.first, p.second, !ok)
                    autoBackup()
                    showStudents()
                }
                setOnLongClickListener {
                    if (ok) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("جزئیات پرداخت")
                            .setMessage("دانش‌آموز: " + s.name + "\nماه: " + months[mi] + "\nتاریخ پرداخت: " + (pay?.date ?: "—"))
                            .setPositiveButton("بستن", null).show()
                        true
                    } else false
                }
            }
            r.addView(v, LinearLayout.LayoutParams(dp(70), dp(46)).apply { setMargins(dp(1), dp(1), dp(1), dp(1)) })
        }
        r.setOnLongClickListener { details(s); true }
        return r
    }

    private fun cellLight(p: LinearLayout, s: String, w: Int, bold: Boolean = false) {
        p.addView(text(s, 11f, navy, bold).apply {
            gravity = Gravity.CENTER
            background = shape(Color.WHITE, 5, line)
        }, LinearLayout.LayoutParams(dp(w), dp(46)).apply { setMargins(dp(1), dp(1), dp(1), dp(1)) })
    }

    private fun addStudent() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        val name = EditText(this).apply {
            hint = "مثال: علی رضایی"
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val fee = EditText(this).apply {
            hint = "مثال: 500,000"
            inputType = InputType.TYPE_CLASS_NUMBER
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        form.addView(text("نام دانش‌آموز *", 13f, navy, true))
        form.addView(name, LinearLayout.LayoutParams(-1, dp(56)).apply { setMargins(0, dp(3), 0, dp(8)) })
        form.addView(text("مبلغ ماهانه (تومان) *", 13f, navy, true))
        form.addView(fee, LinearLayout.LayoutParams(-1, dp(56)).apply { setMargins(0, dp(3), 0, dp(8)) })
        AlertDialog.Builder(this)
            .setTitle("افزودن دانش‌آموز")
            .setView(form)
            .setPositiveButton("ذخیره") { _, _ ->
                if (name.text.isNotBlank()) {
                    db.addStudent(name.text.toString().trim(), fee.text.toString().replace(",", "").toLongOrNull() ?: 0L)
                    autoBackup()
                    showDashboard()
                }
            }
            .setNegativeButton("انصراف", null).show()
    }

    private fun details(s: Student) {
        val ay = academicYear()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val h = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        h.addView(text("👤", 34f, blue, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(58), dp(65)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(text(s.name, 18f, navy, true))
        info.addView(text("مبلغ ماهانه: " + money(s.fee) + " تومان\nسال تحصیلی: " + ay, 12f, Color.DKGRAY))
        h.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(h)

        val t = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.WHITE, 12, line)
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        t.addView(text("تاریخچه پرداخت‌ها", 15f, navy, true))
        t.addView(tableHeader3("ماه", "وضعیت", "تاریخ پرداخت"))
        PersianCalendar.academicMonths(startYear).forEachIndexed { i, p ->
            val pay = db.payment(s.id, ay, p.first, p.second)
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            simpleCell(row, months[i], .34f)
            simpleCell(row, if (pay?.paid == true) "✓ پرداخت شده" else "− پرداخت نشده", .33f, if (pay?.paid == true) green else red)
            simpleCell(row, pay?.date ?: "—", .33f)
            t.addView(row)
        }
        box.addView(t)
        AlertDialog.Builder(this).setTitle("جزئیات دانش‌آموز").setView(box).setPositiveButton("بستن", null).show()
    }

    private fun tableHeader3(a: String, b: String, c: String): View {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        simpleCell(r, a, .34f, Color.WHITE, blue)
        simpleCell(r, b, .33f, Color.WHITE, blue)
        simpleCell(r, c, .33f, Color.WHITE, blue)
        return r
    }

    private fun simpleCell(p: LinearLayout, s: String, w: Float, color: Int = navy, bgColor: Int = Color.WHITE) {
        p.addView(text(s, 11f, color, true).apply {
            gravity = Gravity.CENTER
            background = shape(bgColor, 4, line)
        }, LinearLayout.LayoutParams(0, dp(36), w).apply { setMargins(dp(1), dp(1), dp(1), dp(1)) })
    }

    private fun showMenu() {
        val items = arrayOf("دانش‌آموزان", "افزودن دانش‌آموز", "پشتیبان‌گیری", "بازیابی", "گزارش ماهانه", "تنظیمات")
        AlertDialog.Builder(this).setTitle("منوی برنامه").setItems(items) { _, i ->
            when (i) {
                0 -> showStudents()
                1 -> addStudent()
                2 -> backupExport()
                3 -> restoreFile()
                4 -> monthlyReport()
                5 -> settings()
            }
        }.show()
    }

    private fun monthlyReport() {
        val ay = academicYear()
        val st = db.students()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pairs = PersianCalendar.academicMonths(startYear)
        var paid = 0
        var received = 0L
        st.forEach { s -> pairs.forEach { p -> if (db.payment(s.id, ay, p.first, p.second)?.paid == true) { paid++; received += s.fee } } }
        val total = st.size * 12
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statCard(stats, "✓", "پرداخت شده", paid.toString(), green)
        statCard(stats, "+", "پرداخت نشده", (total - paid).coerceAtLeast(0).toString(), red)
        statCard(stats, "₿", "دریافت شده", money(received), teal, "تومان")
        statCard(stats, "▣", "مانده", money((st.sumOf { it.fee } * 12 - received).coerceAtLeast(0)), purple, "تومان")
        box.addView(stats, LinearLayout.LayoutParams(-1, dp(110)))

        val table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = shape(Color.WHITE, 12, line) }
        val h = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        simpleCell(h, "نام دانش‌آموز", .40f, Color.WHITE, blue)
        simpleCell(h, "مبلغ", .25f, Color.WHITE, blue)
        simpleCell(h, "وضعیت", .35f, Color.WHITE, blue)
        table.addView(h)
        val today = PersianCalendar.today()
        st.forEach { s ->
            val pay = db.payment(s.id, ay, today.y, today.m)
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            simpleCell(row, s.name, .40f)
            simpleCell(row, money(s.fee), .25f)
            simpleCell(row, if (pay?.paid == true) "پرداخت شده" else "پرداخت نشده", .35f, if (pay?.paid == true) green else red)
            row.setOnClickListener { details(s) }
            table.addView(row)
        }
        box.addView(table, LinearLayout.LayoutParams(-1, dp(360)).apply { setMargins(0, dp(10), 0, 0) })
        box.addView(text("⇩  خروجی گزارش", 14f, blue, true).apply {
            gravity = Gravity.CENTER
            background = shape(Color.rgb(232, 244, 255), 12)
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(0, dp(10), 0, 0) })
        AlertDialog.Builder(this).setTitle("گزارش ماهانه").setView(box).setPositiveButton("بستن", null).show()
    }

    private fun settings() {
        val items = arrayOf(
            "اطلاعات برنامه\nنسخه 1.0.0",
            "مدیریت سال تحصیلی\n" + academicYear(),
            "تنظیمات نمایش\nزبان: فارسی | تم: روشن",
            "درباره\nمدیریت سرویس مدرسه"
        )
        AlertDialog.Builder(this).setTitle("تنظیمات").setItems(items, null)
            .setNegativeButton("خروج از برنامه") { _, _ -> finish() }.show()
    }

    private fun backupExport() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "school-service-backup.json")
        }, 10)
    }

    private fun restoreFile() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }, 11)
    }

    override fun onActivityResult(rc: Int, res: Int, data: Intent?) {
        super.onActivityResult(rc, res, data)
        if (res != RESULT_OK || data?.data == null) return
        try {
            if (rc == 10) {
                contentResolver.openOutputStream(data.data!!)?.use { it.write(db.exportJson().toByteArray(Charsets.UTF_8)) }
            } else if (rc == 11) {
                contentResolver.openInputStream(data.data!!)?.bufferedReader(Charsets.UTF_8)?.use { db.restoreJson(it.readText()) }
                autoBackup()
                showDashboard()
            }
            Toast.makeText(this, "عملیات با موفقیت انجام شد", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "فایل پشتیبان معتبر نیست؛ اطلاعات فعلی حفظ شد", Toast.LENGTH_LONG).show()
        }
    }

    private fun bottomNav(active: Int): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = shape(Color.WHITE, 16, line)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val labels = arrayOf("خانه", "دانش‌آموزان", "گزارش‌ها", "تنظیمات")
        labels.forEachIndexed { i, l ->
            val v = text(if (i == active) "●  " + l else l, 11f, if (i == active) blue else Color.GRAY, true)
            v.gravity = Gravity.CENTER
            v.setOnClickListener { when (i) { 0 -> showDashboard(); 1 -> showStudents(); 2 -> monthlyReport(); 3 -> settings() } }
            nav.addView(v, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
        return nav
    }

    private fun academicYear() = startYear.toString() + "-" + (startYear + 1)

    private fun money(v: Long) = String.format(Locale.US, "%,d", v)

    private fun autoBackup() {
        getSharedPreferences("backup", 0).edit().putString("json", db.exportJson()).apply()
    }

    override fun onPause() {
        super.onPause()
        autoBackup()
    }
}
