package com.example.projectplanner.dto;

public class AssigneeDTO {
    private Long userid;
    private String name;

    public AssigneeDTO(Long userid, String name) {
        this.userid = userid;
        this.name = name;
    }

    public Long getUserid() { return userid; }
    public void setUserid(Long userid) { this.userid = userid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
