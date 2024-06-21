import java.time.LocalDateTime;

public class testRLL {
    public static void main(String[] args) {
        RequestLinkedList linkedList = new RequestLinkedList();

        // Example Requests
        Request request1 = new Request("1", "query1", null, LocalDateTime.now(), null, Request.RequestStatus.PENDING, Request.RequestType.LIST_UNIQUE_WORDS, 5);
    
        Request request2 = new Request("2", "query2", null, LocalDateTime.now(), null, Request.RequestStatus.PENDING, Request.RequestType.FIND_DOCUMENTS, 10);
    
        Request request3 = new Request("3", "query3", null, LocalDateTime.now(), null, Request.RequestStatus.PENDING, Request.RequestType.LIST_UNIQUE_WORDS, 3);
    

        // Insert requests
        linkedList.insert(request1);
        linkedList.insert(request2);
        linkedList.insert(request3);
        linkedList.printList();
    }
}
