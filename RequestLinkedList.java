class RequestNode {
    Request data;
    RequestNode next;

    public RequestNode(Request data) {
        this.data = data;
        this.next = null;
    }
}

public class RequestLinkedList {
    private RequestNode head;

    public RequestLinkedList() {
        this.head = null;
    }

  
    public void insert(Request newRequest) {
        RequestNode newNode = new RequestNode(newRequest);
        if (head == null || head.data.getNumFiles() <= newRequest.getNumFiles()) {
            newNode.next = head;
            head = newNode;
        } else {
            RequestNode current = head;
            while (current.next != null && current.next.data.getNumFiles() > newRequest.getNumFiles()) {
                current = current.next;
            }
            newNode.next = current.next;
            current.next = newNode;
        }
    }

    public boolean remove(String requestId) {
        if (head == null) {
            return false; // List is empty
        }

 
        if (head.data.getRequestId().equals(requestId)) {
            head = head.next;
            return true;
        }

        RequestNode current = head;
        while (current.next != null && !current.next.data.getRequestId().equals(requestId)) {
            current = current.next;
        }

        if (current.next == null) {
            return false;
        }

    
        current.next = current.next.next;
        return true;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public Request poll() {
        if (head == null) {
            return null; 
        }

        RequestNode nodeToPoll = head;
        head = head.next;
        return nodeToPoll.data;
    }

    public void printList() {
        RequestNode current = head;
        while (current != null) {
            System.out.println(current.data.getRequestId() + ": " + current.data.getNumFiles());
            current = current.next;
        }
    }
}
