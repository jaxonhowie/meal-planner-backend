package com.mealplanner.dto;

import lombok.Data;

import java.util.List;

@Data
public class FamilyDto {
    private Long id;
    private String name;
    private String inviteCode;
    private List<MemberDto> members;

    @Data
    public static class MemberDto {
        private Long id;
        private String username;
        private String nickname;
    }
}
