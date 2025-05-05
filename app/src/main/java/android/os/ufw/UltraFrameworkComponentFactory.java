package android.os.ufw;

public interface UltraFrameworkComponentFactory {
    static UltraFrameworkComponentFactory getInstance() {
        try {
            return UltraFrameworkComponentFactoryImpl.getInstance();
        } catch (Throwable throwable) {
            return null;
        }
    }
} 