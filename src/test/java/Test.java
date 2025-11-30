//public class Test {
//    int x;
//    public Test() { this.x = 13; }
//    public static void main(String[] args) {
//        Test t = new Test();
//        System.out.println(t.x);
//    }
//}

//public class Test {
//    public Test() { System.out.println(123); }
//    public static void main(String[] args) { new Test(); }
//}

public class Test {
    int x;

    public static void main(String[] args) {
        Test o = new Test();
        o.x = 42;
        System.out.println(o.x);
    }
}
