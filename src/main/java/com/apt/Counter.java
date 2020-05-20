package com.apt;

public class Counter {
    CountHolder countHolder = new CountHolder();

    public Counter(int initialCount) {
        countHolder.setCount(initialCount);
    }

    public void increment(){
        synchronized (countHolder) {
            int count = countHolder.getCount();
            countHolder.setCount(count + 1);
        }
    }

    public void decrement(){
        synchronized (countHolder) {
            int count = countHolder.getCount();
            countHolder.setCount(count + 1);
        }
    }

    public void check(int numUnprocessed) {
        synchronized (countHolder){
            try{
                while(numUnprocessed <= countHolder.getCount()){
                    countHolder.wait();
                }
            }catch (Exception e){
//                e.printStackTrace();
            }
        }

    }

    public void uncheck() {
        synchronized (countHolder){
            try{
                countHolder.notifyAll();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public int getCount() {
        return countHolder.getCount();
    }
}
