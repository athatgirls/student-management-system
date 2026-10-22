package org.example.service;

import org.example.dto.StudentRef;

import java.util.Map;

/**
 * 学生身份解析的唯一入口。标识约定见 {@link StudentRef}。
 *
 * <p>与 {@link StudentService} 的分工：学生数据的增删改查归 StudentService；
 * 本接口只负责「当前是谁、记录挂到谁名下」，返回的 {@link StudentRef}
 * 是业务记录三个标识字段（studentAccountId / studentId / studentName）的唯一写入来源。
 *
 * <p>Controller/Service 需要学生身份时一律经由本接口取得，
 * 禁止在业务代码里直接把 JWT claim 或学号当关联键使用。
 */
public interface StudentRefService {

    /**
     * 当前登录学生身份（要求学生登录态）。
     * 返回的 accountId 可直接写入业务记录的 studentAccountId。
     */
    StudentRef current(Map<String, Object> currentUser);

    /**
     * 管理端/导入按学号指定目标学生，内部完成学号 → 账号主键解析，
     * 调用方禁止自行转换。查不到抛 {@code BusinessException(STUDENT_NOT_FOUND)}。
     */
    StudentRef resolveByStudentId(String studentId);

    /** 按账号主键解析学生身份。 */
    StudentRef resolveByAccountId(String accountId);
}
