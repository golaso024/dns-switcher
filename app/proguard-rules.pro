# Удаляем вызовы Log из релиза (меньше кода и нет расходов на логирование)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
