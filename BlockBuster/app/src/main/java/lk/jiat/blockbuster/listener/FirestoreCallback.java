package lk.jiat.blockbuster.listener;

public interface FirestoreCallback<T> {
    void onCallback(T data);
}
