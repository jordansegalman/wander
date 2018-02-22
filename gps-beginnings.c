/* NOTE: the code below is a framework for the future implementation of GPS-storage.
 *  TODO: Implement data pushing to database
 *	 Define data var to hold recieved GPS data
 */

#define MAX_TIME_INTERVAL 1000

typedef struct node{
	int data;
	int date;
	struct node* next;
	struct node* prev;
}

node* head;
node* tail;

//Creates node with cordinate data
int create(int data, int date){
	node* new_node = (node*)malloc(sizeof(node));
	if(new_node == NULL){
		printf("Error in Node creation.\n");
		exit(0);
	}
	new_node->data = data;
	new_node->date = date;
	if(head != NULL)
		head->prev = new_node;
	new_node->next = head;
	new_node->prev = NULL;
	head = new_node;
	removeOld();
	return 1;
}

//removes the tail nodes that are outdated
int removeOld(){
	while(checkOld()){
		tail = tail->prev;
		free(tail->next);
		tail->next = NULL;
	}
return 1;
}

//checks if the tail node is outdated and returns 1 if true
int checkOld(){
	if(head->date - tail->date > MAX_TIME_INTERVAL)
		return 1;
	return 0;
}

int main(){
	create(0);
	tail = head;
}
