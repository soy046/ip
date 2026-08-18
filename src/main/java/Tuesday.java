import java.util.Scanner;

public class Tuesday {
    public static void main(String[] args) {
        // initialize the input, scanner and the task array with the index for input
        Scanner sc = new Scanner(System.in);
        String input = "";
        String[] taskArray = new String[100];
        int index = 0;

        // greetings part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.banner);
        System.out.println(Strings.greeting);
        System.out.println(Strings.horizontalLine);

        // task adding part
        input = sc.nextLine();
        while (! input.equals("bye")) {
            taskArray[index] = input;
            index++;
            System.out.println(Strings.horizontalLine);
            System.out.println("added: " + input);
            System.out.println(Strings.horizontalLine);
            input = sc.nextLine();
        }

        // farewell part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.farewell);
        System.out.println(Strings.horizontalLine);
    }
}
