import java.util.Scanner;

/**
 * Runs the chat bot
 */
public class Tuesday {
    /**
     * Starts the chatbot, store tasks and process the user input
     *
     * @param args
     */
    public static void main(String[] args) {
        // initialize the input, scanner and the task array with the index for input
        Scanner sc = new Scanner(System.in);
        String input = "";
        Task[] taskArray = new Task[100];
        int taskCount = 0;

        // greetings part
        System.out.println(Strings.horizontalLine);
        System.out.println(Strings.banner);
        System.out.println(Strings.greeting);
        System.out.println(Strings.horizontalLine);

        // task adding part and tasks listing part
        input = sc.nextLine();
        while (! input.equals("bye")) {
            // task listing part
            if (input.equals("list")) {
                System.out.println(Strings.horizontalLine);
                for (int i = 1; i < taskCount + 1; i++) {
                    System.out.println(i + "." + taskArray[i - 1].toString());
                }
                System.out.println(Strings.horizontalLine);
                input = sc.nextLine();
                continue;
            }
            // task adding part
            taskArray[taskCount] = new Task(input);
            taskCount++;
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
