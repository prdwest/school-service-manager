package ir.prdwest.schoolservicemanager
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
 private lateinit var db: DatabaseHelper
 private lateinit var content: LinearLayout
 private lateinit var yearSpinner: Spinner
 private var startYear = 0
 private val months = PersianCalendar.months
 private val navy=Color.rgb(25,45,75); private val blue=Color.rgb(30,110,200)
 private val green=Color.rgb(31,150,95); private val red=Color.rgb(205,65,70)
 private val bg=Color.rgb(246,248,252); private val line=Color.rgb(220,226,235)

 override fun onCreate(b:Bundle?) {
  super.onCreate(b); window.decorView.layoutDirection=View.LAYOUT_DIRECTION_RTL
  db=DatabaseHelper(this); val t=PersianCalendar.today()
  startYear=if(t.m<=6)t.y-1 else t.y; buildUi()
 }
 private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
 private fun rounded(c:Int,r:Int=14,s:Int?=null)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();s?.let{setStroke(dp(1),it)}}
 private fun label(s:String,size:Float=15f,color:Int=navy,bold:Boolean=false)=TextView(this).apply{
  text=s;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL
  if(bold)typeface=Typeface.DEFAULT_BOLD;setPadding(dp(10),dp(7),dp(10),dp(7))
 }
 private fun button(s:String,icon:String="",click:()->Unit)=TextView(this).apply{
  text=if(icon.isBlank())s else icon+"  "+s;textSize=14f;setTextColor(navy);gravity=Gravity.CENTER
  typeface=Typeface.DEFAULT_BOLD;background=rounded(Color.WHITE,12,line);setPadding(dp(12),dp(9),dp(12),dp(9));setOnClickListener{click()}
 }
 private fun buildUi(){
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(12),dp(10),dp(12),dp(10))}
  val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  val title=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  title.addView(label("دانش‌آموزان",25f,navy,true));title.addView(label("مدیریت پرداخت سرویس مدرسه",13f,Color.DKGRAY))
  header.addView(title,LinearLayout.LayoutParams(0,-2,1f))
  header.addView(TextView(this).apply{text="☰";textSize=27f;gravity=Gravity.CENTER;setTextColor(navy);setOnClickListener{showMenu()}},LinearLayout.LayoutParams(dp(55),dp(55)))
  root.addView(header)
  val yr=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;background=rounded(Color.WHITE,14,line);setPadding(dp(8),dp(4),dp(8),dp(4))}
  yr.addView(label("سال تحصیلی",13f,Color.DKGRAY,true),LinearLayout.LayoutParams(dp(90),-2))
  yearSpinner=Spinner(this);val years=(PersianCalendar.today().y-2..PersianCalendar.today().y+1).map{it.toString()+"–"+(it+1)}
  yearSpinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,years)
  yearSpinner.setSelection(years.indexOf(startYear.toString()+"–"+(startYear+1)).coerceAtLeast(0))
  yearSpinner.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
   override fun onNothingSelected(p:AdapterView<*>?){};override fun onItemSelected(p:AdapterView<*>?,v:View?,pos:Int,id:Long){startYear=years[pos].substringBefore("–").toInt();refreshTable()}
  }
  yr.addView(yearSpinner,LinearLayout.LayoutParams(0,-2,1f));root.addView(yr,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(10),0,dp(10))})
  content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  root.addView(ScrollView(this).apply{addView(content)},LinearLayout.LayoutParams(-1,0,1f));setContentView(root);refreshTable()
 }
 private fun refreshTable(){
  content.removeAllViews();val st=db.students();val ay=startYear.toString()+"-"+(startYear+1);val pairs=PersianCalendar.academicMonths(startYear)
  var paid=0;var received=0L
  st.forEach{s->pairs.forEach{p->if(db.payment(s.id,ay,p.first,p.second)?.paid==true){paid++;received+=s.fee}}}
  val total=st.size*12
  val stats=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  stat(stats,"پرداخت‌شده",paid.toString(),green);stat(stats,"پرداخت‌نشده",(total-paid).coerceAtLeast(0).toString(),red)
  stat(stats,"دریافتی",money(received),blue);stat(stats,"مانده",money((st.sumOf{it.fee}*12-received).coerceAtLeast(0)),Color.rgb(170,110,35))
  content.addView(stats,LinearLayout.LayoutParams(-1,dp(78)).apply{setMargins(0,0,0,dp(10))})
  content.addView(button("افزودن دانش‌آموز","+"){addStudent()},LinearLayout.LayoutParams(-1,dp(48)).apply{setMargins(0,0,0,dp(10))})
  val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=rounded(Color.WHITE,16,line);setPadding(dp(8),dp(8),dp(8),dp(8))}
  card.addView(label("جدول دانش‌آموزان",18f,navy,true));card.addView(label("✓ پرداخت شده   — پرداخت نشده | برای تغییر روی خانه ماه بزنید",12f,Color.DKGRAY))
  val table=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};table.addView(headerRow())
  st.forEachIndexed{i,s->table.addView(studentRow(i+1,s,ay,pairs))}
  if(st.isEmpty())table.addView(label("هنوز دانش‌آموزی ثبت نشده است. از افزودن دانش‌آموز شروع کنید.",15f,Color.DKGRAY))
  card.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=true;addView(table)},LinearLayout.LayoutParams(-1,dp(420)))
  content.addView(card)
 }
 private fun stat(parent:LinearLayout,t:String,v:String,c:Int){
  val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;background=rounded(Color.WHITE,13,line)}
  b.addView(label(t,11f,Color.DKGRAY,true).apply{gravity=Gravity.CENTER});b.addView(label(v,14f,c,true).apply{gravity=Gravity.CENTER})
  parent.addView(b,LinearLayout.LayoutParams(0,-1,1f).apply{setMargins(dp(3),0,dp(3),0)})
 }
 private fun headerRow():View{val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};cell(r,"ردیف",58,navy,true);cell(r,"دانش‌آموز",150,navy,true);cell(r,"شهریه",105,navy,true);months.forEach{cell(r,it,78,navy,true)};return r}
 private fun studentRow(n:Int,s:Student,ay:String,pairs:List<Pair<Int,Int>>):View{
  val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(if(n%2==0)Color.rgb(250,251,253)else Color.WHITE)}
  cell(r,n.toString(),58,navy,false);cell(r,s.name,150,navy,true);cell(r,money(s.fee),105,Color.DKGRAY,false)
  pairs.forEachIndexed{mi,p->
   val pay=db.payment(s.id,ay,p.first,p.second);val isPaid=pay?.paid==true
   val v=TextView(this).apply{text=if(isPaid)"✓" else "—";textSize=if(isPaid)23f else 18f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER
    setTextColor(if(isPaid)green else Color.LTGRAY);background=rounded(if(isPaid)Color.rgb(232,248,239)else Color.rgb(248,249,251),9,line)
    setOnClickListener{db.setPaid(s.id,ay,p.first,p.second,!isPaid);autoBackup();refreshTable()}
    setOnLongClickListener{if(isPaid)AlertDialog.Builder(this@MainActivity).setTitle("جزئیات پرداخت").setMessage("دانش‌آموز: "+s.name+"\nماه: "+months[mi]+"\nتاریخ پرداخت: "+(pay?.date?:"—")).setPositiveButton("بستن",null).show();true}
   };r.addView(v,LinearLayout.LayoutParams(dp(78),dp(50)).apply{setMargins(dp(2),dp(3),dp(2),dp(3))})
  };return r
 }
 private fun cell(p:LinearLayout,s:String,w:Int,c:Int,b:Boolean){p.addView(label(s,13f,c,b).apply{gravity=Gravity.CENTER;background=rounded(Color.rgb(244,247,251),8,line)},LinearLayout.LayoutParams(dp(w),dp(50)).apply{setMargins(dp(2),dp(3),dp(2),dp(3))})}
 private fun addStudent(){
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val name=EditText(this).apply{hint="نام و نام خانوادگی";textSize=16f}
  val fee=EditText(this).apply{hint="شهریه ماهانه (تومان)";inputType=InputType.TYPE_CLASS_NUMBER;textSize=16f};box.addView(name);box.addView(fee)
  AlertDialog.Builder(this).setTitle("افزودن دانش‌آموز").setView(box).setPositiveButton("ذخیره"){_,_->if(name.text.isNotBlank()){db.addStudent(name.text.toString().trim(),fee.text.toString().toLongOrNull()?:0L);autoBackup();refreshTable()}}.setNegativeButton("انصراف",null).show()
 }
 private fun showMenu(){
  val items=arrayOf("دانش‌آموزان","افزودن دانش‌آموز","پشتیبان‌گیری","بازیابی","گزارش ماهانه")
  AlertDialog.Builder(this).setTitle("منوی برنامه").setItems(items){_,w->when(w){0->refreshTable();1->addStudent();2->backupExport();3->restoreFile();4->monthlyReport()}}.show()
 }
 private fun backupExport(){startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="application/json";putExtra(Intent.EXTRA_TITLE,"school-service-backup.json")},10)}
 private fun restoreFile(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/json";addCategory(Intent.CATEGORY_OPENABLE)},11)}
 override fun onActivityResult(rc:Int,res:Int,data:Intent?){super.onActivityResult(rc,res,data);if(res!=RESULT_OK||data?.data==null)return;try{
  if(rc==10)contentResolver.openOutputStream(data.data!!)?.use{it.write(db.exportJson().toByteArray(Charsets.UTF_8))}
  else if(rc==11){contentResolver.openInputStream(data.data!!)?.bufferedReader(Charsets.UTF_8)?.use{db.restoreJson(it.readText())};autoBackup();refreshTable()}
  Toast.makeText(this,"عملیات با موفقیت انجام شد",Toast.LENGTH_SHORT).show()
 }catch(_:Exception){Toast.makeText(this,"فایل پشتیبان معتبر نیست؛ اطلاعات فعلی حفظ شد",Toast.LENGTH_LONG).show()}}
 private fun monthlyReport(){
  val ay=startYear.toString()+"-"+(startYear+1);val sb=StringBuilder("گزارش سال تحصیلی "+ay+"\n\n")
  db.students().forEach{s->sb.append("◆ ").append(s.name).append(" — ").append(money(s.fee)).append(" تومان\n");PersianCalendar.academicMonths(startYear).forEachIndexed{i,p->{val pay=db.payment(s.id,ay,p.first,p.second);sb.append(months[i]).append(": ").append(if(pay?.paid==true)"✓ پرداخت شده — "+pay.date else "— پرداخت نشده").append("\n")}};sb.append("\n")}
  AlertDialog.Builder(this).setTitle("گزارش ماهانه").setMessage(sb.toString()).setPositiveButton("بستن",null).show()
 }
 private fun money(v:Long)=String.format("%,d",v)
 private fun autoBackup(){getSharedPreferences("backup",0).edit().putString("json",db.exportJson()).apply()}
 override fun onPause(){super.onPause();autoBackup()}
}
