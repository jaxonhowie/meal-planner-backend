package com.mealplanner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealplanner.dto.FamilyDto;
import com.mealplanner.entity.Family;
import com.mealplanner.entity.User;
import com.mealplanner.mapper.FamilyMapper;
import com.mealplanner.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyMapper familyMapper;
    private final UserMapper userMapper;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 创建家庭并让当前用户加入 */
    @Transactional
    public FamilyDto create(Long userId, String familyName) {
        User user = userMapper.selectById(userId);
        if (user.getFamilyId() != null) throw new RuntimeException("你已经在一个家庭中");

        Family family = new Family();
        family.setName(familyName);
        family.setInviteCode(generateCode());
        familyMapper.insert(family);

        user.setFamilyId(family.getId());
        userMapper.updateById(user);

        return buildDto(family, List.of(user));
    }

    /** 通过邀请码加入家庭 */
    @Transactional
    public FamilyDto join(Long userId, String inviteCode) {
        User user = userMapper.selectById(userId);
        if (user.getFamilyId() != null) throw new RuntimeException("你已经在一个家庭中，请先退出");

        Family family = familyMapper.selectOne(new LambdaQueryWrapper<Family>()
            .eq(Family::getInviteCode, inviteCode.toUpperCase()));
        if (family == null) throw new RuntimeException("邀请码无效");

        user.setFamilyId(family.getId());
        userMapper.updateById(user);

        return getFamily(userId);
    }

    /** 查询当前家庭信息 */
    public FamilyDto getFamily(Long userId) {
        User user = userMapper.selectById(userId);
        if (user.getFamilyId() == null) throw new RuntimeException("你还没有加入家庭");

        Family family = familyMapper.selectById(user.getFamilyId());
        List<User> members = userMapper.selectList(new LambdaQueryWrapper<User>()
            .eq(User::getFamilyId, family.getId()));

        return buildDto(family, members);
    }

    /** 退出家庭 */
    @Transactional
    public void leave(Long userId) {
        User user = userMapper.selectById(userId);
        if (user.getFamilyId() == null) return;
        user.setFamilyId(null);
        userMapper.updateById(user);
    }

    // ── 工具方法 ──────────────────────────────

    private String generateCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            code = sb.toString();
        } while (familyMapper.selectCount(new LambdaQueryWrapper<Family>()
                .eq(Family::getInviteCode, code)) > 0);
        return code;
    }

    private FamilyDto buildDto(Family family, List<User> members) {
        FamilyDto dto = new FamilyDto();
        dto.setId(family.getId());
        dto.setName(family.getName());
        dto.setInviteCode(family.getInviteCode());
        dto.setMembers(members.stream().map(m -> {
            FamilyDto.MemberDto md = new FamilyDto.MemberDto();
            md.setId(m.getId());
            md.setUsername(m.getUsername());
            md.setNickname(m.getNickname());
            return md;
        }).collect(Collectors.toList()));
        return dto;
    }
}
