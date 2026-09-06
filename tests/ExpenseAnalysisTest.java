import com.example.expensemate.*;
import java.math.BigDecimal;
import java.util.*;

/** Standalone JVM regression checks; no device data is modified. */
public class ExpenseAnalysisTest {
    static long time(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, 0);
        return c.getTimeInMillis();
    }
    static Expense expense(long id, long amount, long at, String... tags) {
        return new Expense(id, amount, Arrays.asList(tags), "备注", at);
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        long now = time(2026, 9, 6, 12);
        List<Expense> data = Arrays.asList(
            expense(1, 100, time(2026, 9, 1, 0), "餐饮", "午餐", "餐饮"),
            expense(2, 300, now),
            expense(3, 900, now + 1),
            expense(4, 200, time(2026, 8, 26, 0), "餐饮"),
            expense(5, 400, time(2026, 8, 31, 12), "餐饮"),
            expense(6, 800, time(2026, 8, 31, 13), "餐饮")
        );
        ExpenseAnalysis month = new ExpenseAnalysis(data, AnalysisPeriod.MONTH, null, now);
        check(month.getTotal().equals(new BigDecimal(400)), "month boundaries and future exclusion");
        check(month.getPreviousTotal().equals(new BigDecimal(600)), "equal elapsed comparison boundaries");
        check(month.getDays() == 6 && month.getActiveDays() == 2, "natural and active day counts");
        check(month.getTrend().size() == 6 && month.getTrend().get(1).getTotal().signum() == 0, "zero-filled daily trend");
        check(month.getTags().stream().filter(b -> b.getLabel().equals("餐饮")).findFirst().get().getTotal().intValue() == 100, "deduplicate tags per expense");
        check(new ExpenseAnalysis(data, AnalysisPeriod.MONTH, "餐饮", now).getTotal().intValue() == 100, "tag filtering");
        check(new ExpenseAnalysis(data, AnalysisPeriod.MONTH, "未分类", now).getExpenses().size() == 1, "untagged records");
        check(month.getWeekdays().get(6).getTotal().intValue() == 300, "Sunday bucket");
        check(month.getHours().get(2).getTotal().intValue() == 300, "noon belongs to afternoon");
        check(new ExpenseAnalysis(data, AnalysisPeriod.WEEK, null, now).getStart() == time(2026, 8, 31, 0), "Monday week start");
        check(new ExpenseAnalysis(data, AnalysisPeriod.DAYS_30, null, now).getDays() == 30, "rolling 30 days");
        ExpenseAnalysis empty = new ExpenseAnalysis(Collections.emptyList(), AnalysisPeriod.ALL, null, now);
        check(empty.getDays() == 1 && empty.getTotal().signum() == 0, "empty history");
        ExpenseAnalysis huge = new ExpenseAnalysis(Arrays.asList(expense(1, Long.MAX_VALUE, now), expense(2, Long.MAX_VALUE, now)), AnalysisPeriod.MONTH, null, now);
        check(huge.getTotal().equals(BigDecimal.valueOf(Long.MAX_VALUE).multiply(BigDecimal.valueOf(2))), "aggregate cannot overflow Long");
        ExpenseAnalysis leap = new ExpenseAnalysis(Collections.emptyList(), AnalysisPeriod.YEAR, null, time(2024, 3, 1, 12));
        check(leap.getDays() == 61 && leap.getTrend().size() == 3, "leap year and monthly trend");
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        ExpenseAnalysis dst = new ExpenseAnalysis(Collections.emptyList(), AnalysisPeriod.DAYS_30, null, time(2026, 3, 15, 12));
        check(dst.getDays() == 30 && dst.getTrend().size() == 30, "DST uses calendar days");
        System.out.println("PASS: 15 analysis regression checks");
    }
}
