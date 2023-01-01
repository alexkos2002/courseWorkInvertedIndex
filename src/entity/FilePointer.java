package entity;

import java.util.Objects;

public class FilePointer implements Comparable<FilePointer>{

    private String path;

    private long size;

    public FilePointer(String path, long size) {
        this.path = path;
        this.size = size;
    }

    public FilePointer() {

    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public int compareTo(FilePointer filePointer) {
        if (this.size > filePointer.size) {
            return 1;
        } else if (this.size < filePointer.size) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FilePointer)) return false;
        FilePointer that = (FilePointer) o;
        return size == that.size && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, size);
    }

    @Override
    public String toString() {
        return String.format("File %s: %d bytes", path, size);
    }
}
