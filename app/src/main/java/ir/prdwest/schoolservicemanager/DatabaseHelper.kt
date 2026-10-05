package ir.prdwest.schoolservicemanager
import android.content.*
import android.database.sqlite.*
data class Student(val id:Long,val name:String,val fee:Long)
data class Payment(val paid:Boolean,val date:String?)
class DatabaseHelper(c:Context):SQLiteOpenHelper(c,"school_service.db",null,1){
 override fun onCreate(d:SQLiteDatabase){d.execSQL("CREATE TABLE students(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,fee INTEGER NOT NULL DEFAULT 0)");d.execSQL("CREATE TABLE payments(student_id INTEGER,academic_year TEXT,year INTEGER,month INTEGER,paid INTEGER,date TEXT,PRIMARY KEY(student_id,academic_year,year,month))")}
 override fun onUpgrade(d:SQLiteDatabase,o:Int,n:Int){}
 fun students():List<Student>{val c=readableDatabase.rawQuery("SELECT id,name,fee FROM students ORDER BY name",null);val r=mutableListOf<Student>();while(c.moveToNext())r.add(Student(c.getLong(0),c.getString(1),c.getLong(2)));c.close();return r}
 fun addStudent(n:String,f:Long){val v=ContentValues();v.put("name",n);v.put("fee",f);writableDatabase.insert("students",null,v)}
 fun payment(s:Long,a:String,y:Int,m:Int):Payment?{val c=readableDatabase.rawQuery("SELECT paid,date FROM payments WHERE student_id=? AND academic_year=? AND year=? AND month=?",arrayOf(s.toString(),a,y.toString(),m.toString()));val p=if(c.moveToFirst())Payment(c.getInt(0)==1,c.getString(1))else null;c.close();return p}
 fun setPaid(s:Long,a:String,y:Int,m:Int,paid:Boolean){val v=ContentValues();v.put("student_id",s);v.put("academic_year",a);v.put("year",y);v.put("month",m);v.put("paid",if(paid)1 else 0);if(paid)v.put("date",PersianCalendar.format(PersianCalendar.today()))else v.putNull("date");writableDatabase.insertWithOnConflict("payments",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
 fun exportJson():String{val o=org.json.JSONObject();val a=org.json.JSONArray();students().forEach{s->val x=org.json.JSONObject();x.put("id",s.id);x.put("name",s.name);x.put("fee",s.fee);a.put(x)};o.put("students",a);val p=org.json.JSONArray();val c=readableDatabase.rawQuery("SELECT student_id,academic_year,year,month,paid,date FROM payments",null);while(c.moveToNext()){val x=org.json.JSONObject();x.put("student_id",c.getLong(0));x.put("academic_year",c.getString(1));x.put("year",c.getInt(2));x.put("month",c.getInt(3));x.put("paid",c.getInt(4));x.put("date",if(c.isNull(5))org.json.JSONObject.NULL else c.getString(5));p.put(x)};c.close();o.put("payments",p);return o.toString()}
 fun restoreJson(j:String){
  val o=org.json.JSONObject(j)
  val students=o.getJSONArray("students")
  val payments=o.getJSONArray("payments")
  val validStudents=mutableListOf<Triple<Long,String,Long>>()
  val ids=HashSet<Long>()
  for(i in 0 until students.length()){
   val x=students.getJSONObject(i)
   val id=x.getLong("id"); val name=x.getString("name").trim(); val fee=x.getLong("fee")
   if(id<=0L || name.isEmpty() || fee<0L || !ids.add(id)) throw IllegalArgumentException("پشتیبان نامعتبر است")
   validStudents.add(Triple(id,name,fee))
  }
  val validPayments=mutableListOf<Array<Any?>>()
  for(i in 0 until payments.length()){
   val x=payments.getJSONObject(i)
   val sid=x.getLong("student_id"); val ay=x.getString("academic_year")
   val y=x.getInt("year"); val m=x.getInt("month"); val paid=x.getInt("paid")
   if(!ids.contains(sid) || ay.isBlank() || y<1200 || m !in 1..12 || paid !in 0..1) throw IllegalArgumentException("پشتیبان نامعتبر است")
   val date=if(x.isNull("date")) null else x.getString("date")
   validPayments.add(arrayOf(sid,ay,y,m,paid,date))
  }
  val d=writableDatabase
  d.beginTransaction()
  try{
   d.delete("payments",null,null); d.delete("students",null,null)
   for(s in validStudents){val v=ContentValues();v.put("id",s.first);v.put("name",s.second);v.put("fee",s.third);d.insertOrThrow("students",null,v)}
   for(p in validPayments){val v=ContentValues();v.put("student_id",p[0] as Long);v.put("academic_year",p[1] as String);v.put("year",p[2] as Int);v.put("month",p[3] as Int);v.put("paid",p[4] as Int);if(p[5]==null)v.putNull("date")else v.put("date",p[5] as String);d.insertOrThrow("payments",null,v)}
   d.setTransactionSuccessful()
  }finally{d.endTransaction()}
 }
}