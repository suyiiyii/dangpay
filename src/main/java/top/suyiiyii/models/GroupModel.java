package top.suyiiyii.models;

import lombok.Data;
import top.suyiiyii.su.orm.annotation.ColumnSetting;
import top.suyiiyii.su.orm.annotation.TableRegister;

import java.io.Serializable;

@Data
@TableRegister("group")
public class GroupModel implements Serializable {
    @ColumnSetting(isPrimaryKey = true, isAutoIncrement = true)
    private int id;
    @ColumnSetting(stringLength = 100, isNotNull = true, isUnique = true)
    private String name;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    private String pepoleCount;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    private String enterpriseScale;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    private String industry;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    private String address;
    @ColumnSetting(stringLength = 100, isNotNull = true)
    private String contact;
    @ColumnSetting(stringLength = 20, isNotNull = true)
    private String status;
    @ColumnSetting(stringLength = 20, isNotNull = true)
    private String hide;

}
