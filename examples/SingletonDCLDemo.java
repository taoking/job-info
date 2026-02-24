public class SingletonDCLDemo {

    private static volatile SingletonDCLDemo INSTANCE;

    private SingletonDCLDemo() {
    }

    public static SingletonDCLDemo getInstance() {
        if (INSTANCE == null) {
            synchronized (SingletonDCLDemo.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SingletonDCLDemo();
                }
            }
        }
        return INSTANCE;
    }

    public static void main(String[] args) {
        SingletonDCLDemo s1 = SingletonDCLDemo.getInstance();
        SingletonDCLDemo s2 = SingletonDCLDemo.getInstance();
        System.out.println("same instance? " + (s1 == s2));
    }
}
