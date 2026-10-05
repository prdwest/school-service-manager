package ir.prdwest.schoolservicemanager
data class JDate(val y:Int,val m:Int,val d:Int)
object PersianCalendar {
 val months=listOf("مهر","آبان","آذر","دی","بهمن","اسفند","فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور")
 fun today():JDate{val c=java.util.Calendar.getInstance();return fromGregorian(c.get(1),c.get(2)+1,c.get(5))}
 fun fromGregorian(gy:Int,gm:Int,gd:Int):JDate{val gdm=intArrayOf(0,31,59,90,120,151,181,212,243,273,304,334);val jy0=if(gy>1600)979 else 0;val gy0=if(gy>1600)gy-1600 else gy-621;val gy2=if(gm>2)gy0+1 else gy0;var days=365*gy0+(gy2+3)/4-(gy2+99)/100+(gy2+399)/400-80+gd+gdm[gm-1];var jy=jy0+33*(days/12053);days%=12053;jy+=4*(days/1461);days%=1461;if(days>365){jy+=(days-1)/365;days=(days-1)%365};val jm=if(days<186)1+days/31 else 7+(days-186)/30;val jd=1+(if(days<186)days%31 else(days-186)%30);return JDate(jy,jm,jd)}
 fun academicYearFor(j:JDate)=if(j.m<=6)(j.y-1).toString()+"-"+j.y else j.y.toString()+"-"+(j.y+1)
 fun academicMonths(start:Int)=List(12){i->if(i<6)Pair(start,i+7)else Pair(start+1,i-5)}
 fun format(j:JDate)="%04d/%02d/%02d".format(j.y,j.m,j.d)
}
