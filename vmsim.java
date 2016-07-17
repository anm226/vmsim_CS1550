
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/*
 * @author Andrew Masih
 * @email anm226@pitt.edu
 */
public class vmsim {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        int numOfFrames = 0;
        String algorithm = "";
        int refreshTime = 0;
        String fileTrace  = "";
        String n = "-n";
        String a = "-a";
        String r = "-r";

        for(int i = 0; i<args.length; i++){
            if(args[i].equals(n)){
                i++;
                numOfFrames = Integer.parseInt(args[i]);
                i++;
            }
            if(args[i].equals(a)){
                i++;
                algorithm = args[i];
                i++;
            }
            if(args[i].equals(r)){
                i++;
                refreshTime = Integer.parseInt(args[i]);
                i++;
            }

            fileTrace = args[i];
        }


        String opt = "opt";
        String clock = "clock";
        String lru = "lru";
        String aging = "aging";
        if(algorithm.equals(opt)){
          Opt algo = new Opt(numOfFrames, fileTrace);
          algo.runOpt();
        }
        else if(algorithm.equals(clock)){
          SecondChance algo = new SecondChance(numOfFrames, fileTrace);
          algo.runSC();
        }
        else if(algorithm.equals(lru)){
          LRU algo = new LRU(numOfFrames, fileTrace);
          algo.runLRU();
        }
        else if(algorithm.equals(aging)){
          Aging algo = new Aging(numOfFrames, fileTrace, refreshTime);
          algo.runAging();
        }
    }



}
/*
 The implementation of opt page replacement algorithm.
 */

class Opt{
    private int numberOfPages = 1048576;
    //Queue to keep track when the next memory access of a certain page will be made.
    RefQueue[] nextUse = new RefQueue[numberOfPages];
    PageTable table;
    //Frame table mimics the RAM, keeping the pages in use.
    int[] frameTable;
    //This variable will be used to keep index in
    //frame table, as to which page will be replaced.
    private int index;
    //The next two variables will keep the stats recorded.
    private int numOfPageFaults = 0;
    private int numOfDiskWrites = 0;
    //Keep the value of number of frames given by user.
    private int numOfFrames;
    //This Object converts hexa values to decimal
    private HexConverter h2d = new HexConverter();
    String file;
    private int numOfAddresses;

    //Initializes the page table and frame table.
    //The frame table is initialzed with -1s,
    //saying there are no pages loaded.
    public Opt(int num, String fileName){
        //initializes new page table to be used
        this.table = new PageTable();
        this.numOfFrames = num;
        this.frameTable = new int[numOfFrames];
        for(int i = 0; i<numOfFrames; i++){
            this.frameTable[i] = -1;
        }
        file = fileName;
    }
    public boolean runOpt(){

        populateRefTrace();

        try{
            //Things needed to read the file.
            File newFile = new File(file);
            FileReader fileReader = new FileReader(newFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            int frameNum = 0;

            while((line = bufferedReader.readLine()) != null){
                String strNum = line.substring(0, Math.min(line.length(), 5));
                int pageNum = h2d.hex2decimal(strNum);
                PTE entry = table.getEntry(pageNum);
                String RW = (line.substring(line.length() - 1));
                String check = "W";
                //Check if memory address is dirty, if it is
                //go into the page table and make the dirty bit 1.
                if(RW.equals(check)){
                     entry.dBit = 1;
                }
                //Remove this from occurence from the queue.
                nextUse[pageNum].remove();

                //If the page being accessed is not already in frameTable.
                if(checkIfInFT(pageNum)==false){
                    //If the frame table is full.

                    if(frameNum==numOfFrames){

                        //Figure out what page to evict from page table.
                        int pageToEvict = checkPageToEvict();
                        //Get that page.
                        PTE entryDel = table.getEntry(pageToEvict);

                        //If dBit = 1, write it to disk.
                        if(entryDel.dBit==1){
                            System.out.println("Page Fault: Eviction Page Dirty");
                            numOfDiskWrites++;
                            entryDel.dBit = 0;
                        }
                        else{
                          System.out.println("Page Fault: Eviction Page Clean");
                        }

                        //Replace the page to be evicted with the page accessed right now.
                        frameTable[index] = pageNum;
                    }
                    //Put the first pages in frame table, while the frame table is not full.
                    else{
                      System.out.println("Page Fault: No Eviction");
                      frameTable[frameNum] = pageNum;
                      frameNum++;
                    }
                }else{
                  System.out.println("No Page Fault: HIT!");
                }
                numOfAddresses++;
        }
        printStats();
        return true;
        }
         catch(IOException e) {
			e.printStackTrace();
        }
        return false;
    }
    /*
    This method populates the data structure that makes keeps the data regarding
    when a memory refrence will be made next for opt to have perfect knowledge.
    */
    public void populateRefTrace(){
         try{
            File newFile = new File(file);
            FileReader fileReader = new FileReader(newFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            int i = 0;
            while((line = bufferedReader.readLine()) != null){

                String strNum = line.substring(0, Math.min(line.length(), 5));
                int pageNum = h2d.hex2decimal(strNum);
                if(nextUse[pageNum]==null){
                    nextUse[pageNum] = new RefQueue();
                    nextUse[pageNum].add(i);
                }
                else{
                    nextUse[pageNum].add(i);
                }
                String RW = (line.substring(line.length() - 1));
                int valid = 1;
                int rBit = 1;
                int dBit;
                String check = "R";
                if(RW.equals(check)){
                    dBit = 0;
                }
                else{
                    dBit = 1;
                }

                table.addEntry(pageNum, -1, valid, rBit, dBit, -1);
                i++;
            }
        }
        catch(IOException e) {
			e.printStackTrace();
        }
    }

    /*
      This method just goes through the frame table and checks if the page
      being demanded at the moment is in the frame table. If it's not,
      increase the number of page faults by 1.
    */
    public boolean checkIfInFT(int pageNum){
        for(int i = 0; i<numOfFrames; i++){
            if(frameTable[i]==pageNum){
                return true;
            }
        }
        numOfPageFaults++;
        return false;
    }
    /*
     This method uses the queue data structure to figure out when a certain
     address/page will be accesed next and according to that removes the
     page that will be accessed the farthest away.
    */
    public int checkPageToEvict(){
        int pageEvict = 0;
        int peek = 0;
           for(int i=0; i<numOfFrames; i++){
               int temp = nextUse[frameTable[i]].peek();
               //System.out.println("t " + temp);
               if(peek<temp){
                   peek=temp;
                   pageEvict = frameTable[i];
                   index = i;
               }
               else if(temp==-1){
                   peek=temp;
                   pageEvict = frameTable[i];
                   index = i;
                   break;
               }

           }

        return pageEvict;
    }
    /*
     Prints the stats out to screen.
     */
    public void printStats(){
      System.out.println("------------------------------------------");
      System.out.println("Algorithm: opt");
      System.out.println("Number of Frames: " + numOfFrames);
      System.out.println("Total Memory Accesses: " + numOfAddresses);
      System.out.println("Total Page Faults: "+ numOfPageFaults);
      System.out.println("Total Writes to Disk: "+ numOfDiskWrites);
      System.out.println("------------------------------------------");
    }
}


/*
  The second chance algorithm implemented using the clock reference scheme.
*/
class SecondChance{
    //Frame table
    private int[] frameClock;
    String file;
    HexConverter h2d = new HexConverter();
    private int numOfFrames;
    private int numOfPageFaults;
    private int numOfDiskWrites;
    //This variable points to the correct index in the clock.
    private int clockPointer;
    PageTable table = new PageTable();
    private int numOfMem = 0;
    //Second Chance constructor.
    public SecondChance(int num, String fileName){
        this.numOfFrames = num;
        this.frameClock = new int[numOfFrames];
        this.file = fileName;
    }
    public boolean runSC(){
        try{
            File newFile = new File(file);
            FileReader fileReader = new FileReader(newFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            int frameNum = 0;

            while((line = bufferedReader.readLine()) != null){
                String strNum = line.substring(0, Math.min(line.length(), 5));
                int pageNum = h2d.hex2decimal(strNum);
                PTE curEntry = table.getEntry(pageNum);

                //If the memory address being accessed is already in the page t
                //table make its rBit 1, and check if it's a dirty page and
                //set the dBit accordingly.
                if(curEntry!=null){
                    curEntry.rBit = 1;
                    String RW = (line.substring(line.length() - 1));

                    String check = "W";
                    if(RW.equals(check)){
                        curEntry.dBit = 1;
                    }

                }
                //Else if the entry is not in the page table add it.
                //addEntry(); method returns null, if the page is already in
                //the page table.
                String RW = (line.substring(line.length() - 1));
                int validBit = 1;
                int rBit = 1;
                int dBit;
                String check = "R";
                if(RW.equals(check)){
                    dBit = 0;
                }
                else{
                    dBit = 1;
                }
                table.addEntry(pageNum, frameNum, validBit, rBit, dBit, -1);
                //If the page table is not in the frame table.
                if(checkIfInFT(pageNum)==false){
                    //If the frame table is full.
                    if(numOfFrames==frameNum){
                        int pointer = pageToEvict();

                        //Frame clock is implemented using an array so, if the
                        //current pointer is pointing to 0, it means it is
                        //actually referring to the 8th element.
                        if(pointer==0){
                            pointer=numOfFrames;
                        }
                        PTE pageEvict = table.getEntry(frameClock[pointer-1]);
                        //If the dBit is 1, write it to disk.
                        if(pageEvict.dBit==1){
                            System.out.println("Page Fault: Eviction Page Dirty!");
                            numOfDiskWrites++;
                            pageEvict.dBit = 0;
                        }
                        else{
                          System.out.println("Page Fault: Eviction Page Clean!");
                        }
                         frameClock[pointer-1] = pageNum;
                 }
                 else{
                      System.out.println("Page Fault: No Eviction!");
                     frameClock[frameNum] = pageNum;
                     frameNum++;
                 }
              }
              else{
                System.out.println("No Page Fault: HIT!");
              }
             numOfMem++;
        }
        printStats();
        return true;
        }
        catch(IOException e) {
			e.printStackTrace();
        }

        return false;
    }

    /*
      This method just goes through the frame table and checks if the page
      being demanded at the moment is in the frame table. If it's not,
      increase the number of page faults by 1.
    */
    public boolean checkIfInFT(int pageNum){
        for(int i = 0; i<numOfFrames; i++){
            if(frameClock[i]==pageNum){
                return true;
            }
        }
        numOfPageFaults++;
        return false;
    }

    /*
     Finds the correct page to evict and returns it. Giving each page that
     is currently referenced another chance.
    */
    public int pageToEvict(){
        int pageNum;
        while(true){
            pageNum = frameClock[clockPointer];
            PTE entry = table.getEntry(pageNum);
            if(entry.rBit==1){
                entry.rBit =0;
            }
            else{
                clockPointer = (clockPointer+1)%numOfFrames;

                return clockPointer;
            }
            clockPointer = (clockPointer+1)%numOfFrames;

        }
    }
    /*
     Prints the stats out to screen.
     */
    public void printStats(){
      System.out.println("------------------------------------------");
      System.out.println("Algorithm: clock");
      System.out.println("Number of Frames: " + numOfFrames);
      System.out.println("Total Memory Accesses: " + numOfMem);
      System.out.println("Total Page Faults: "+ numOfPageFaults);
      System.out.println("Total Writes to Disk: "+ numOfDiskWrites);
      System.out.println("------------------------------------------");
    }
}


/*
 The implementation of least recently used algorithm.
*/
class LRU{
    private int[] frameTable;
    String file;
    HexConverter h2d = new HexConverter();
    private int numOfFrames;
    private int numOfPageFaults;
    private int numOfDiskWrites;
    private int timeStamp;
    private int index;
    PageTable table = new PageTable();
    public LRU(int num, String fileName){
        this.file = fileName;
        this.numOfFrames = num;

        this.frameTable = new int[numOfFrames];
    }
    /*
     Follows a similar structure to clock but implements the replacing algorithm
     Least Recently Used.  Time stamp represents when the address/page was loaded
     into memory. Using that time stamp, the algorithm evicts the correct page.
     Time stamp is incremented every memory trace.
    */
    public boolean runLRU(){
       try{
            File newFile = new File(file);
            FileReader fileReader = new FileReader(newFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            int frameNum = 0;
            while((line = bufferedReader.readLine()) != null){
                String strNum = line.substring(0, Math.min(line.length(), 5));
                int pageNum = h2d.hex2decimal(strNum);
                PTE curEntry = table.getEntry(pageNum);
                if(curEntry!=null){
                    curEntry.timeStamp = timeStamp;
                    String RW = (line.substring(line.length() - 1));

                    String check = "W";
                    if(RW.equals(check)){
                        curEntry.dBit = 1;
                    }
                }
                String RW = (line.substring(line.length() - 1));
                int validBit = 1;
                int rBit = 1;
                int dBit = 0;
                String check = "W";
                if(RW.equals(check)){
                    dBit = 1;
                }
                table.addEntry(pageNum, frameNum, validBit, rBit, dBit, timeStamp);
                if(checkIfInFT(pageNum)==false){

                    if(numOfFrames==frameNum){
                        int pageToEvict = pageToEvict();
                        PTE page = table.getEntry(pageToEvict);
                        if(page.dBit==1){
                            System.out.println("Page Fault: Eviction Page Dirty!");
                            numOfDiskWrites++;
                            page.dBit = 0;
                        }
                        else{
                            System.out.println("Page Fault: Eviction Page Clean!");
                        }
                        frameTable[index] = pageNum;
                    }
                 else{
                     System.out.println("Page Fault: No Eviction!");
                     frameTable[frameNum] = pageNum;
                     frameNum++;
                 }
             }
             else{
               System.out.println("No Page Fault : HIT!");
             }
             timeStamp++;
        }
        printStats();
        return true;
        }
        catch(IOException e) {
			e.printStackTrace();
        }

        return false;


    }
    public boolean checkIfInFT(int pageNum){
        for(int i = 0; i<numOfFrames; i++){
            if(frameTable[i]==pageNum){
                return true;
            }
        }
        numOfPageFaults++;
        return false;
    }

    /*
     Basically checks which page has the least stamp number and
     makes it the pageToEvict.
    */
    public int pageToEvict(){
        int page = 0;
        int pageToEvict = 0;
        int tS = timeStamp;
        int temp = 0;

        for(int i = 0; i<numOfFrames; i++){
            page = frameTable[i];
            PTE entry = table.getEntry(page);
            temp = entry.timeStamp;
            if(temp<=tS){
                index = i;
                tS=temp;
                pageToEvict = page;
            }
        }
        return pageToEvict;
    }
    /*
     Prints the stats out to screen.
     */
    public void printStats(){
      System.out.println("------------------------------------------");
      System.out.println("Algorithm: lru");
      System.out.println("Number of Frames: " + numOfFrames);
      System.out.println("Total Memory Accesses: " + timeStamp);
      System.out.println("Total Page Faults: "+ numOfPageFaults);
      System.out.println("Total Writes to Disk: "+ numOfDiskWrites);
      System.out.println("------------------------------------------");
    }
}


/*
 The implementation of aging page replacement algorithm.
 */
 class Aging{

     String file;
     HexConverter h2d = new HexConverter();
     private int numOfFrames;
     private int numOfPageFaults;
     private int numOfDiskWrites;
     private int timeStamp;
     private int bitIndex;
     PageTable table = new PageTable();
     private int[] frameTable;
     //This represetns the 8-bit history table.
     private int[] bits;
     private int refresh;
     private int numOfMem = 0;
     private int refreshCounter;
     public Aging(int num, String fileName, int refresh){
         this.numOfFrames = num;
         this.file = fileName;
         this.refresh = refresh;
         this.frameTable = new int[numOfFrames];
         for(int i = 0; i<numOfFrames; i++){
            frameTable[i] = -1;
         }
         bits = new int[numOfFrames];
     }
     public boolean runAging(){
         try{
             File newFile = new File(file);
             FileReader fileReader = new FileReader(newFile);
             BufferedReader bufferedReader = new BufferedReader(fileReader);
             String line;
             int frameNum = 0;
             while((line = bufferedReader.readLine()) != null){
                 String strNum = line.substring(0, Math.min(line.length(), 5));
                 int pageNum = h2d.hex2decimal(strNum);
                 PTE curEntry = table.getEntry(pageNum);
                 //If this page is already in page table.
                 if(curEntry!=null){
                     //Set rBit to 1, because it is being accessed.
                     curEntry.rBit = 1;
                     //Set dBit to 1, if it is a dirty page.
                     String RW = (line.substring(line.length() - 1));
                     String check = "W";
                     if(RW.equals(check)){
                         curEntry.dBit = 1;
                     }
                 }
                 //If this page is not in the PageTable, add it to the page table.
                 String RW = (line.substring(line.length() - 1));
                 int rBit = 1;
                 int valid = 1;
                 int dBit = 0;
                 String check = "W";
                     if(RW.equals(check)){
                         dBit = 1;
                     }
                 table.addEntry(pageNum, -1, valid, rBit, dBit, -1);

                 //If this page table is not in the frameTable already.
                 //Page Fault occurs.
                 if(checkIfInFT(pageNum)==false){

                     //If frameTable is full, we need to evict a page.
                     if(frameNum==numOfFrames){
                         //Figure out which page to evict.
                         int pageIndex = pageToEvict();
                         PTE entryEvict = table.getEntry(frameTable[pageIndex]);
                         //If the page being evicted is dirty, increment
                         //number of diskWrites, and set dBit to 0.
                         if(entryEvict.dBit==1){
                             System.out.println("Page Fault: Eviction Page Dirty");
                             numOfDiskWrites++;
                             entryEvict.dBit = 0;
                         }
                         else{
                            System.out.println("Page Fault: Eviction Page Clean");
                         }
                         //Set rBit to 0, since it's being evcited.
                         entryEvict.rBit = 0;

                         //Clear this page's history from the 8-bit history table.
                         clearHistory(pageIndex);

                         //Add the new page to the frame table.
                         frameTable[pageIndex] = pageNum;
                     }
                     //else we just add the page to the frameTable.
                     else{
                     System.out.println("Page Fault: No Eviction");
                     frameTable[frameNum] = pageNum;
                     frameNum++;
                     }
                 }
                 else{
                   System.out.println("No Page Fault: HIT!");
                 }

                 //Increment the refresh counter.
                 refreshCounter++;

                 //If refresh counter equals the time set for refresh,
                 //run upon refresh method.
                 if(refreshCounter==refresh){
                     uponRefresh();
                     //Reset refresh counter
                     refreshCounter = 0;
                 }
                 //Increment number of memory address accesses.
                 numOfMem++;
             }

            //After going through all the addresses print out the stats.
            printStats();

         }
         catch(IOException e) {
             e.printStackTrace();
         }
         return false;
     }

     /*This method basically goes through the frameTable and checks
      *if the page we are trying to demand is already in the frameTable.
      *If it's not in the frameTable, pageFaults is incremented.
      */
     public boolean checkIfInFT(int pageNum){

         for(int i = 0; i<numOfFrames; i++){
             if(frameTable[i]==pageNum){
                 bitIndex = i;
                 return true;
             }
         }
         numOfPageFaults++;
         return false;
     }

     /*This method basically clears the rBit of a page in
      *the frameTable(RAM), and puts that value in the most significant
      *bit of the 8-bit history table.
      */
     public void uponRefresh(){
         int page;
         //Shift the bits before adding the new value.
         shiftBit();
         for(int i = 0; i<numOfFrames; i++){
             page = frameTable[i];
             if(page!=-1){
             PTE pageEntry = table.getEntry(page);
             if(pageEntry.rBit==1){
                 bits[i] = (bits[i] | (1 << 7));
             }
             else{
                 bits[i] = (bits[i] & ~(1 << 7));
             }
             //Set rbit to 0 after storing it to 8-bit history table.
             pageEntry.rBit = 0;
             }

         }
     }

     /*This method goes in and picks the correct page to evict, it checks
      *which page has the smallest "value" in the 8-bit history table,
      *it also makes sure that, the page it is picking has a rbit value of 0.
      *If it can't pick a page depending on that criteria, it picks 0th page in
      *in the page table. For the method to not be able to pick a value based on
      *the first criteria is very rare.
      */
     public int pageToEvict(){
         int pageIndex = 0;
         int value = 255;

         boolean picked =false;
         for(int i=0; i<numOfFrames; i++){
             int temp = bits[i];
             PTE pageEntry = table.getEntry(frameTable[i]);
             if((temp<=value)&&(pageEntry.rBit==0)){
                 value = temp;
                 picked = true;
                 pageIndex = i;
             }

         }
         if(picked){
           return pageIndex;
         }
         else{
            return 0;
         }
     }
     /*This method clears the 8-bit history of page
      *after it is evicted.
      */
     public void clearHistory(int index){
         int clear = 0;
         bits[index] = clear;
     }
     /*
         Shifts the bytes in byte array 1 bit prior to writing the
         rBit to it upon refresh.
     */
     public void shiftBit(){
        for(int i = 0; i<numOfFrames; i++){
            bits[i]= bits[i]>>>1;
        }

     }
     /*This method just prints out the stats.
      */
     public void printStats(){
       System.out.println("------------------------------------------");
       System.out.println("Algorithm: aging");
       System.out.println("Number of Frames: " + numOfFrames);
       System.out.println("Total Memory Accesses: " + numOfMem);
       System.out.println("Total Page Faults: "+ numOfPageFaults);
       System.out.println("Total Writes to Disk: "+ numOfDiskWrites);
       System.out.println("Refresh Rate: "+ refresh);
       System.out.println("------------------------------------------");
     }
 }


/*
  This class represents a page table which has enough entries to support a 32 bit address space.
*/
class PageTable{
    private int pageSize = 4096;
    private int numberOfPages = 1048576;
    private PTE[] table;

    public PageTable(){
       this.table = new PTE[numberOfPages];

    }
    public PTE addEntry(int pageNum, int frameNum, int validBit, int rBit, int dBit, int timeStamp){
        if(this.table[pageNum]==null){

            PTE newEntry = new PTE(frameNum, validBit, rBit, dBit, timeStamp);
            this.table[pageNum] = newEntry;
            return newEntry;
        }
        return null;
    }
    public PTE getEntry(int pageNum){
        return this.table[pageNum];
    }


}

/*
This represents a page table entry, with all of its components.
*/
class PTE{
    public int frameNumber;
    public int validBit;
    public int rBit;
    public int dBit;
    //Only used for LRU
    public int timeStamp;
    public PTE(int frameNumber, int validBit, int rBit, int dBit, int timeStamp){

        this.frameNumber = frameNumber;
        this.validBit = validBit;
        this.rBit = rBit;
        this.dBit = dBit;
        this.timeStamp = timeStamp;
    }
}

/*
 This is basically a queue that keeps integers representing when will a certain
 page be accessed next.  This is mainly used for the opt algorithm.
*/
class RefQueue{
    private Queue<Integer> queRef;

    public RefQueue(){
        this.queRef = new ConcurrentLinkedQueue<Integer>();
    }
    public void add(int acess){
        this.queRef.add(acess);
    }
    public int remove(){
       if(!this.queRef.isEmpty()){
        int ret  = this.queRef.remove();
        return ret;
        }
       return -1;
    }
    public int peek(){
       if(this.queRef.peek()==null){
            return -1;
       }
       int ret =  this.queRef.peek();
       return ret;
    }
    public boolean isEmpty(){
        if(this.queRef.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }
    public void printQueue(){
        while(!(this.queRef.isEmpty())){
            System.out.println(this.queRef.remove());
        }
    }
}
/*
    This class converts hexadecmial number to decimal for the use of ease.
    I got this class from:
    http://introcs.cs.princeton.edu/java/31datatype/HexConverter.java.html
    */
class HexConverter {
    public HexConverter(){};
    public int hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }
}
