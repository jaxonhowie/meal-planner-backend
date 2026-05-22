package com.mealplanner.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mealplanner.dto.FamilyDto;
import com.mealplanner.entity.Family;
import com.mealplanner.entity.User;
import com.mealplanner.mapper.FamilyMapper;
import com.mealplanner.mapper.UserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @Mock
    private FamilyMapper familyMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FamilyService familyService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Family.class);
    }

    @Test
    void create_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setNickname("测试用户");
        user.setFamilyId(null);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(familyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(familyMapper.insert(any(Family.class))).thenAnswer(invocation -> {
            Family f = invocation.getArgument(0);
            f.setId(10L);
            return 1;
        });
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        FamilyDto result = familyService.create(1L, "我的家庭");

        assertThat(result.getName()).isEqualTo("我的家庭");
        assertThat(result.getMembers()).hasSize(1);
        assertThat(result.getMembers().get(0).getUsername()).isEqualTo("testuser");
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void create_alreadyInFamily() {
        User user = new User();
        user.setId(1L);
        user.setFamilyId(10L);

        when(userMapper.selectById(1L)).thenReturn(user);

        assertThatThrownBy(() -> familyService.create(1L, "我的家庭"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("你已经在一个家庭中");
    }

    @Test
    void join_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setFamilyId(null);

        Family family = new Family();
        family.setId(10L);
        family.setName("已有家庭");
        family.setInviteCode("ABC12345");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(familyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(family);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(familyMapper.selectById(10L)).thenReturn(family);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user));

        FamilyDto result = familyService.join(1L, "ABC12345");

        assertThat(result.getId()).isEqualTo(10L);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void join_alreadyInFamily() {
        User user = new User();
        user.setId(1L);
        user.setFamilyId(10L);

        when(userMapper.selectById(1L)).thenReturn(user);

        assertThatThrownBy(() -> familyService.join(1L, "ABC12345"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("你已经在一个家庭中，请先退出");
    }

    @Test
    void join_invalidCode() {
        User user = new User();
        user.setId(1L);
        user.setFamilyId(null);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(familyMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> familyService.join(1L, "INVALID1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("邀请码无效");
    }

    @Test
    void getFamily_success() {
        User user = new User();
        user.setId(1L);
        user.setFamilyId(10L);

        Family family = new Family();
        family.setId(10L);
        family.setName("我的家庭");
        family.setInviteCode("ABC12345");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(familyMapper.selectById(10L)).thenReturn(family);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user));

        FamilyDto result = familyService.getFamily(1L);

        assertThat(result.getName()).isEqualTo("我的家庭");
        assertThat(result.getInviteCode()).isEqualTo("ABC12345");
    }

    @Test
    void getFamily_noFamily() {
        User user = new User();
        user.setId(1L);
        user.setFamilyId(null);

        when(userMapper.selectById(1L)).thenReturn(user);

        assertThatThrownBy(() -> familyService.getFamily(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("你还没有加入家庭");
    }

    @Test
    void leave_success() {
        User user = new User();
        user.setId(1L);
        user.setFamilyId(10L);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        familyService.leave(1L);

        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void leave_noFamily() {
        User user = new User();
        user.setId(1L);
        user.setFamilyId(null);

        when(userMapper.selectById(1L)).thenReturn(user);

        familyService.leave(1L);
        // No exception, no update call
    }
}
