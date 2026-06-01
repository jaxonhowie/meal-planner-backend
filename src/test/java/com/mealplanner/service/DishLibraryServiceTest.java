package com.mealplanner.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mealplanner.entity.DishLibrary;
import com.mealplanner.entity.User;
import com.mealplanner.mapper.DishLibraryMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishLibraryServiceTest {

    @Mock
    private DishLibraryMapper dishLibraryMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private DishLibraryService dishLibraryService;

    @BeforeAll
    static void initMybatisPlusCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, DishLibrary.class);
    }

    private User mockUser(Long id, Long familyId) {
        User user = new User();
        user.setId(id);
        user.setFamilyId(familyId);
        return user;
    }

    @Test
    void add_success() {
        User user = mockUser(1L, 10L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user));
        when(dishLibraryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dishLibraryMapper.insert(any(DishLibrary.class))).thenReturn(1);

        DishLibrary result = dishLibraryService.add(1L, "红烧肉", "dinner", "荤,热菜");

        assertThat(result.getName()).isEqualTo("红烧肉");
        assertThat(result.getMealType()).isEqualTo("dinner");
        assertThat(result.getCheckinCount()).isEqualTo(0);
        verify(dishLibraryMapper).insert(any(DishLibrary.class));
    }

    @Test
    void add_duplicateName() {
        User user = mockUser(1L, 10L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user));
        when(dishLibraryMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> dishLibraryService.add(1L, "红烧肉", "dinner", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("菜品已存在: 红烧肉");
    }

    @Test
    void recordCheckin_existing() {
        DishLibrary existing = new DishLibrary();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setName("红烧肉");
        existing.setCheckinCount(3);

        when(dishLibraryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(dishLibraryMapper.updateById(any(DishLibrary.class))).thenReturn(1);

        dishLibraryService.recordCheckin(1L, "红烧肉", "dinner", "img.jpg");

        assertThat(existing.getCheckinCount()).isEqualTo(4);
        assertThat(existing.getImageUrl()).isEqualTo("img.jpg");
        verify(dishLibraryMapper).updateById(existing);
    }

    @Test
    void recordCheckin_new() {
        when(dishLibraryMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(dishLibraryMapper.insert(any(DishLibrary.class))).thenReturn(1);

        dishLibraryService.recordCheckin(1L, "新菜品", "lunch", null);

        verify(dishLibraryMapper).insert(any(DishLibrary.class));
    }

    @Test
    void delete_success() {
        when(dishLibraryMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        dishLibraryService.delete(1L, 100L);

        verify(dishLibraryMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void toggleFavorite_success() {
        DishLibrary dish = new DishLibrary();
        dish.setId(1L);
        dish.setIsFavorite(false);

        when(dishLibraryMapper.selectById(1L)).thenReturn(dish);
        when(dishLibraryMapper.updateById(any(DishLibrary.class))).thenReturn(1);

        DishLibrary result = dishLibraryService.toggleFavorite(1L);

        assertThat(result.getIsFavorite()).isTrue();
        verify(dishLibraryMapper).updateById(dish);
    }

    @Test
    void toggleFavorite_alreadyFavorite() {
        DishLibrary dish = new DishLibrary();
        dish.setId(1L);
        dish.setIsFavorite(true);

        when(dishLibraryMapper.selectById(1L)).thenReturn(dish);
        when(dishLibraryMapper.updateById(any(DishLibrary.class))).thenReturn(1);

        DishLibrary result = dishLibraryService.toggleFavorite(1L);

        assertThat(result.getIsFavorite()).isFalse();
    }

    @Test
    void updateTags_success() {
        DishLibrary dish = new DishLibrary();
        dish.setId(1L);
        dish.setTags("旧标签");

        when(dishLibraryMapper.selectById(1L)).thenReturn(dish);
        when(dishLibraryMapper.updateById(any(DishLibrary.class))).thenReturn(1);

        DishLibrary result = dishLibraryService.updateTags(1L, "新标签");

        assertThat(result.getTags()).isEqualTo("新标签");
    }

    @Test
    void updateTags_notFound() {
        when(dishLibraryMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> dishLibraryService.updateTags(999L, "标签"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("菜品不存在: 999");
    }

    @Test
    void setImageUrl_success() {
        DishLibrary dish = new DishLibrary();
        dish.setId(1L);

        when(dishLibraryMapper.selectById(1L)).thenReturn(dish);
        when(dishLibraryMapper.updateById(any(DishLibrary.class))).thenReturn(1);

        DishLibrary result = dishLibraryService.setImageUrl(1L, "new.jpg");

        assertThat(result.getImageUrl()).isEqualTo("new.jpg");
    }

    @Test
    void clearImage_success() {
        DishLibrary dish = new DishLibrary();
        dish.setId(1L);
        dish.setImageUrl("old.jpg");

        when(dishLibraryMapper.selectById(1L)).thenReturn(dish);
        when(dishLibraryMapper.updateById(any(DishLibrary.class))).thenReturn(1);

        DishLibrary result = dishLibraryService.setImageUrl(1L, null);

        assertThat(result.getImageUrl()).isNull();
    }

    @Test
    void random_emptyCandidates() {
        User user = mockUser(1L, 10L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user));
        when(dishLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<String> result = dishLibraryService.random(1L, "dinner", 3);

        assertThat(result).isEmpty();
    }

    @Test
    void random_returnsRequestedCount() {
        User user = mockUser(1L, 10L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(user));

        DishLibrary d1 = new DishLibrary(); d1.setName("A"); d1.setCheckinCount(1); d1.setIsFavorite(false);
        DishLibrary d2 = new DishLibrary(); d2.setName("B"); d2.setCheckinCount(2); d2.setIsFavorite(false);
        DishLibrary d3 = new DishLibrary(); d3.setName("C"); d3.setCheckinCount(3); d3.setIsFavorite(false);
        DishLibrary d4 = new DishLibrary(); d4.setName("D"); d4.setCheckinCount(4); d4.setIsFavorite(false);

        when(dishLibraryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(d1, d2, d3, d4));

        List<String> result = dishLibraryService.random(1L, "dinner", 2);

        assertThat(result).hasSize(2);
    }
}
