package com.teamtag.tagweb.domain.notice.DTO;

public class NoticeListDTO {

    public int ID; //고유 ID

    public String title; //글 제목
    public int viewCount; //조회수
    public String writeTime; //작성시간

    public NoticeListDTO(int ID, String title, int viewCount, String writeTime) {
        this.ID = ID;
        this.title = title;
        this.viewCount = viewCount;
        this.writeTime = writeTime;
    }

    private int serialNum; //현재 글 번호

    public int getSerialNum() {
        return serialNum;
    }

    public void setSerialNum(int serialNum) {
        this.serialNum = serialNum;
    }
}
