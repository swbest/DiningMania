package rvrc.geq1917.g34.android.diningmania;

import java.util.Date;

public class PointTransaction extends TransactionHistory{
    private static int total;

    public PointTransaction(String name, Date date) {
        super(name, date);
        total++;
    }

    public static int getTotal() {
        return total;
    }
}
