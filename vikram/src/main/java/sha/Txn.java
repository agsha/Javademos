package sha;

import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class Txn implements Comparable<Txn>{
    String acc = "";
    Long rec = -1L;
    LocalDate tranDate;
    String jrnlNo = "";
    LocalDate postDate;
    String txnType = "";
    String code = "";
    long amnt = -1;
    long bal = -1;
    String tellBr = "";
    String narr = "";
    String check = "";
    private CSVRecord csv;
    static final DateTimeFormatter formatter =
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yyyy").toFormatter();

    public Txn(String acc, String rec, String tranDate, String jrnlNo, String postDate, String txnType, String code, String amnt, String bal, String tellBr, String narr, String check, CSVRecord csv) {
        this.acc = acc;
        this.rec = Long.parseLong(rec);
        this.tranDate = LocalDate.parse(tranDate, formatter);
        this.jrnlNo = jrnlNo;
        this.postDate = LocalDate.parse(postDate, formatter);
        this.txnType = txnType;
        this.code = code;
        this.amnt = Long.parseLong(amnt.replace(",", ""));
        this.bal = Long.parseLong(bal.replace(",", ""));
        this.tellBr = tellBr.trim();
        this.narr = narr.trim().replaceAll("\\s+", " ").toLowerCase();
        this.check = check.trim();
        this.csv = csv;
    }

    @Override
    public String toString() {
        return "Txn{" +
                "acc='" + acc + '\'' +
                ", rec='" + rec + '\'' +
                ", tranDate='" + tranDate + '\'' +
                ", jrnlNo='" + jrnlNo + '\'' +
                ", postDate='" + postDate + '\'' +
                ", txnType='" + txnType + '\'' +
                ", code='" + code + '\'' +
                ", amnt=" + amnt +
                ", bal=" + bal +
                ", tellBr='" + tellBr + '\'' +
                ", narr='" + narr + '\'' +
                ", check='" + check + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Txn txn = (Txn) o;

        return rec.equals(txn.rec);
    }

    @Override
    public int hashCode() {
        return rec.hashCode();
    }

    @Override
    public int compareTo(Txn o) {
        return (int)(rec-o.rec);
    }
}
