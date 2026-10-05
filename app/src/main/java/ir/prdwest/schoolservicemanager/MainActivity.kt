package ir.prdwest.schoolservicemanager
import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.*

class MainActivity : Activity() {
 private lateinit var db: DatabaseHelper
 private lateinit var list: LinearLayout
 private lateinit var ys: Spinner
 private lateinit var ms: Spinner
 private val months = PersianCalendar.months
 private var start = 0
 private var idx = 0

 override fun onCreate(b: Bundle?) {
  super.onCreate(b)
  window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
  db = DatabaseHelper(this)
  val today = PersianCalendar.today()
  start = if (today.m <= 6) today.y - 1 else today.y
  idx = if (today.m >= 7) today.m - 7 else today.m + 5
  ui()
 }

 private fun text(s: String, z: Float = 16f) = TextView(this).apply {
  text = s
  textSize = z
  setPadding(12, 8, 12, 8)
 }

 private fun ui() {
  val root = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   setPadding(12, 12, 12, 12)
  }
  root.addView(text("مدیریت سرویس مدرسه", 23f))
  root.addView(text("سال تحصیلی جاری: " + PersianCalendar.academicYearFor(PersianCalendar.today())))
  val selectors = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

  ys = Spinner(this)
  val years = (PersianCalendar.today().y - 2..PersianCalendar.today().y + 1).map { "$it-${it + 1}" }
  ys.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)
  ys.setSelection(years.indexOf("$start-${start + 1}").coerceAtLeast(0))
  ys.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
   override fun onNothingSelected(parent: AdapterView<*>?) = Unit
   override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    start = years[position].substringBefore("-").toInt()
    refresh()
   }
  }

  ms = Spinner(this)
  ms.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
  ms.setSelection(idx)
  ms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
   override fun onNothingSelected(parent: AdapterView<*>?) = Unit
   override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    idx = position
    refresh()
   }
  }

  selectors.addView(ys, LinearLayout.LayoutParams(0, -2, 1f))
  selectors.addView(ms, LinearLayout.LayoutParams(0, -2, 1f))
  root.addView(selectors)
  root.addView(Button(this).apply {
   text = "+ افزودن بچه جدید"
   setOnClickListener { add() }
  })
  root.addView(Button(this).apply {
   text = "پشتیبان‌گیری / بازیابی"
   setOnClickListener { backup() }
  })
  root.addView(Button(this).apply {
   text = "گزارش پرداخت‌ها"
   setOnClickListener { report() }
  })
  list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
  val scroll = ScrollView(this)
  scroll.addView(list)
  root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
  setContentView(root)
  refresh()
 }

 private fun refresh() {
  list.removeAllViews()
  val p = PersianCalendar.academicMonths(start)[idx]
  list.addView(text("ماه: ${months[idx]} ${p.first}", 19f))
  val ay = "$start-${start + 1}"
  db.students().forEach { student ->
   val pay = db.payment(student.id, ay, p.first, p.second)
   val row = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(8, 5, 8, 5)
   }
   row.addView(text("${student.name} — مبلغ: ${student.fee}", 17f))
   val check = CheckBox(this).apply {
    text = if (pay?.paid == true) "پرداخت شده" else "پرداخت نشده"
    isChecked = pay?.paid == true
    setOnCheckedChangeListener { _, checked ->
     db.setPaid(student.id, ay, p.first, p.second, checked)
     autoBackup()
     refresh()
    }
   }
   row.addView(check)
   if (pay?.paid == true) row.addView(text("تاریخ پرداخت: ${pay.date}", 14f))
   list.addView(row)
  }
 }

 private fun add() {
  val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
  val name = EditText(this).apply { hint = "نام بچه" }
  val fee = EditText(this).apply { hint = "مبلغ ماهانه"; inputType = 2 }
  box.addView(name)
  box.addView(fee)
  AlertDialog.Builder(this).setTitle("افزودن بچه جدید").setView(box)
   .setPositiveButton("ذخیره") { _, _ ->
    if (name.text.isNotBlank()) {
     db.addStudent(name.text.toString().trim(), fee.text.toString().toLongOrNull() ?: 0L)
     autoBackup()
     refresh()
    }
   }.setNegativeButton("انصراف", null).show()
 }

 private fun autoBackup() {
  getSharedPreferences("backup", 0).edit().putString("json", db.exportJson()).apply()
 }

 private fun backup() {
  AlertDialog.Builder(this).setTitle("پشتیبان‌گیری و بازیابی")
   .setItems(arrayOf("ذخیره فایل پشتیبان", "بازیابی از فایل", "بازیابی پشتیبان خودکار")) { _, which ->
    when (which) {
     0 -> startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      type = "application/json"
      putExtra(Intent.EXTRA_TITLE, "school-service-backup.json")
     }, 10)
     1 -> startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      type = "application/json"
      addCategory(Intent.CATEGORY_OPENABLE)
     }, 11)
     2 -> getSharedPreferences("backup", 0).getString("json", null)?.let {
      try { db.restoreJson(it); refresh() }
      catch (_: Exception) { Toast.makeText(this, "پشتیبان خودکار معتبر نیست", Toast.LENGTH_LONG).show() }
     }
    }
   }.show()
 }

 override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  super.onActivityResult(requestCode, resultCode, data)
  if (resultCode != RESULT_OK || data?.data == null) return
  try {
   if (requestCode == 10) {
    contentResolver.openOutputStream(data.data!!)?.use {
     it.write(db.exportJson().toByteArray(Charsets.UTF_8))
    }
   } else if (requestCode == 11) {
    contentResolver.openInputStream(data.data!!)?.bufferedReader(Charsets.UTF_8)?.use {
     db.restoreJson(it.readText())
    }
   }
   autoBackup()
   refresh()
  } catch (_: Exception) {
   Toast.makeText(this, "فایل پشتیبان معتبر نیست", Toast.LENGTH_LONG).show()
  }
 }

 private fun report() {
  val ay = "$start-${start + 1}"
  val sb = StringBuilder("گزارش سال تحصیلی $ay\n\n")
  db.students().forEach { student ->
   sb.append(student.name).append("\n")
   PersianCalendar.academicMonths(start).forEachIndexed { i, p ->
    val payment = db.payment(student.id, ay, p.first, p.second)
    sb.append(months[i]).append(": ")
     .append(if (payment?.paid == true) "پرداخت شده (${payment.date})" else "پرداخت نشده")
     .append("\n")
   }
   sb.append("\n")
  }
  AlertDialog.Builder(this).setTitle("گزارش").setMessage(sb.toString()).setPositiveButton("بستن", null).show()
 }

 override fun onPause() {
  super.onPause()
  autoBackup()
 }
}
