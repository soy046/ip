import java.util.Scanner;

public class Tuesday {
    public static void main(String[] args) {
        // initialize the input and scanner
        Scanner sc = new Scanner(System.in);
        String input = "";

        // greetings part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.banner);
        System.out.println(Strings.greeting);
        System.out.println(Strings.horizontalLine);

        // echoing part
        input = sc.nextLine();
        while (! input.equals("bye")) {
            System.out.println(Strings.horizontalLine);
            System.out.println(input);
            System.out.println(Strings.horizontalLine);
            input = sc.nextLine();
        }

        // farewell part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.farewell);
        System.out.println(Strings.horizontalLine);
    }
}
