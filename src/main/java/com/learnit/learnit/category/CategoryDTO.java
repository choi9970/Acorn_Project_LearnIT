package com.learnit.learnit.category;

import lombok.Data;

@Data
public class CategoryDTO {
    private int categoryId;
    private String name;
    private String iconPath;   // ✅ 추가
}