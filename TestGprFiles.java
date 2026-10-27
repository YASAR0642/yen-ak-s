import java.io.*;
import java.util.*;
import com.ktd.analyzer.gpr.*;
public class TestGprFiles {
  static void ck(boolean v,String s){if(!v)throw new AssertionError(s);}
  static void put16(byte[] a,int i,int v){a[i]=(byte)((v>>8)&255);a[i+1]=(byte)(v&255);}
  public static void main(String[] args)throws Exception{
    ArrayList<GprProtocol.Trace> ts=new ArrayList<>();
    for(int i=0;i<3;i++){short[] arr=new short[100];for(int j=0;j<100;j++)arr[j]=(short)(j*2-i*20);ts.add(new GprProtocol.Trace(i+1,arr));}
    ByteArrayOutputStream o=new ByteArrayOutputStream();
    GprFileIo.writeCsv(o,ts,0.25,0.03);
    GprFileIo.Record rd=GprFileIo.readCsv(new ByteArrayInputStream(o.toByteArray()));
    ck(rd.traces.size()==3,"csv trace count");
    ck(rd.traces.get(2).samples[50]==60,"csv sample roundtrip");
    ck(Math.abs(rd.dtNs-.25)<.0001,"csv ns");
    ck(Math.abs(rd.dxMeters-.03)<.0001,"csv dx");
    ck(GprProtocol.depthMeters(100,1,9,0)>4.9 && GprProtocol.depthMeters(100,1,9,0)<5.1,"ns depth equation");
    byte[] segy=new byte[3200+400+240+100*2];
    put16(segy,3200+16,200); // 200 microseconds SEG-Y header
    put16(segy,3200+20,100);
    put16(segy,3200+24,3);
    int th=3600;put16(segy,th+114,100);
    int d=3840;for(int i=0;i<100;i++)put16(segy,d+i*2,i+11);
    GprFileIo.Record sg=GprFileIo.readSegy(new ByteArrayInputStream(segy));
    ck(sg.traces.size()==1 && sg.traces.get(0).samples[99]==110,"SEG-Y 16-bit");
    ck(sg.dtNs==200000,"SEG-Y interval microseconds converted to ns");
    byte[] marked=segy.clone();
    byte[] warning="C03 SAMPLE INTERVAL HEADER=1 US PLACEHOLDER; REAL ACQUISITION DT UNVERIFIED".getBytes("ISO-8859-1");
    System.arraycopy(warning,0,marked,0,warning.length);
    GprFileIo.Record notCalibrated=GprFileIo.readSegy(new ByteArrayInputStream(marked));
    ck(Double.isNaN(notCalibrated.dtNs),"Placeholder GPR ns must NOT be converted to physical depth");
    System.out.println("OK: CSV roundtrip, SEG-Y int16, physical ns formula, PC SGY time placeholder protection");
  }
}
