import GrupoB.Utils.HashCash;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class TestHashCash {

    /*  NOTES:
        32m27s to generate a block with difficulty 30

        1m34s to generate a block with difficulty 32
        27m42s to generate a block with difficulty 32
        34m50s to generate a block with difficulty 32
    */

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        try {
            String cash = HashCash.mintCash(UUID.randomUUID().toString(), 12).toString();

            HashCash hashCash = new HashCash(cash);

            System.out.println(cash);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();

        Date date = new Date(end - start);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateFormatted = formatter.format(date);

        System.out.println("\nExecution time: "  + dateFormatted);
    }
}
