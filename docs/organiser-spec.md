# 组织者端规格 / Organiser-side spec

**来源**：`Project Backlog table.docx`（以 backlog 为准）+ `User Stories5.docx` / `User Story Group 15.docx` 里 US05–US09 的验收标准 + Zihan Huang persona。
**用途**：前端（React）先用 mock 数据实现；后端按此反推 API 和 SQLite 表。
标 **[PROPOSED]** 是我们自己提议的命名和路径；标 **[OPEN]** 需要团队决定。

主要用户：Jiaxin Liu（吉他社）、Zihan Huang（ACM 社副秘书）。**主要用电脑**（宿舍电脑，网络稳定，很少被打断），手机偶尔用。
Zihan 的痛点：统计到场人数费时间、个人信息不完整 → 名单里要有**姓名 + 学号**。

---

## 1. backlog 对照

| ID | 内容 | P | Size | 谁做 |
|---|---|---|---|---|
| B4 | 登录（学生/组织者/老师） | 1 | M | 共用 |
| B5 | 组织者只能管理**自己俱乐部**的活动 | 1 | M | 后端校验 + 前端只展示自己俱乐部 |
| B6 | 俱乐部成员名单；**每个俱乐部最多 3 名组织者** | 1 | M | 后端；账号管理没做，用 seed data |
| B7 | club-only 活动和报名仅限该俱乐部成员 | 1 | M | 创建表单可选；校验在后端 |
| B8 | 创建公开或 club-only 活动，填写必填信息 | 1 | L | 前端 + 后端 |
| B9 | 校验必填项、容量为正数、日期时间有效 | 1 | M | 前端提示 + 后端校验 |
| B10 | 提交新活动，发布前需老师审核 | 1 | M | 前端 + 后端 |
| B13 | 修改被退回的活动并重新提交 | 2 | M | 前端 + 后端 |
| B14 | 审核通过后生成报名二维码 | 1 | L | 后端生成 token，前端渲染二维码 |
| B20 | 查看报名人数和剩余名额 | 2 | S | 前端 + 后端 |
| B23 | 确认后取消活动；停止新报名 | 1 | M | 前端 + 后端 |
| B24 | 活动取消时给已报名学生发邮件 | 1 | M | 后端（可模拟为站内通知） |
| B25 | 向学生清楚显示已取消活动 | 2 | S | 前端 |
| B26 | 已发布活动的修改作为**新审核项**提交 | 1 | L | 前端 + 后端 |
| B27 | 修改获批前，保持现有已批准详情 | 1 | M | 后端 |
| B28 | 活动时间不变时，自动保留已有报名 | 1 | M | 后端 |
| B29 | 修改获批后给已报名学生发邮件 | 1 | M | 后端（可模拟） |
| B30 | 时间变更获批后，名额先预留，等学生确认 | 1 | L | 后端 + 前端展示 |

---

## 2. 页面与路由 [PROPOSED]

| 路由 | 页面 | 作用 | backlog |
|---|---|---|---|
| `/organiser/activities` | MyActivities | "My Activities"：我俱乐部的所有活动，带状态标记 | B5, B25 |
| `/organiser/activities/new` | ActivityForm | 创建活动：保存草稿 / 提交审核 | B8–B10 |
| `/organiser/activities/:id` | ActivityManage | 状态、老师反馈、二维码、报名人数和名单、取消 | B13, B14, B20, B23 |
| `/organiser/activities/:id/edit` | ActivityForm（编辑模式） | 编辑草稿/被退回的活动；对已发布活动提出修改 | B13, B26 |

组织者登录后默认进入 `/organiser/activities`。导航：My Activities、Create Activity、退出。
如果一个组织者管理多个俱乐部，列表顶部有俱乐部筛选 / 创建时选择俱乐部。

---

## 3. 各页面内容与交互

### 3.1 MyActivities（B5, B25）
- 只显示**自己管理的俱乐部**的活动（直接打开别的俱乐部的 URL 必须失败）。
- 每项：标题、日期时间、状态标记（**文字 + 图标，不能只靠颜色**）、已报/容量。
- 状态：`Draft` 草稿 / `Awaiting review` 待审核 / `Needs changes` 被退回 / `Published` 已发布 / `Cancelled` 已取消。
- 空状态："你还没有创建活动"+ Create Activity 按钮。

### 3.2 ActivityForm（B8, B9, B10）
字段（来自 US05 / User Stories5）：

| 字段 | 规则 |
|---|---|
| 所属俱乐部 `clubId` | 只能选自己管理的俱乐部 |
| 标题 `title` | 必填 |
| 介绍 `description` | 必填 |
| 开始日期时间 `startsAt` | 必填，必须晚于现在 |
| 地点 `location` | 必填 |
| 参加要求 `requirements` | 必填 |
| 人数上限 `capacity` | 必填，**正整数** |
| 报名截止 `registrationDeadline` | 必填，**早于活动开始时间** |
| 可见范围 `visibility` | `public`（公开）或 `club_only`（仅俱乐部成员），默认 public |

- 按钮：**保存草稿**（不校验完整性，可随时保存，重新打开内容完整保留）、**提交审核**（校验全部必填）。
- 校验失败：**错误显示在对应字段旁边**，其他已填内容保留；页面顶部有错误汇总，并把焦点移过去。
- 提交成功：状态变 `Awaiting review`，**此时没有报名页，也没有可用二维码**，不能接受报名。

### 3.3 ActivityManage（B13, B14, B20, B23, B26）
按状态显示不同内容：

| 状态 | 页面内容 |
|---|---|
| `Draft` | 编辑、提交审核 |
| `Awaiting review` | 只读，显示"等待老师审核"；**不能编辑**（避免与审核版本不一致） |
| `Needs changes` | 显示老师的**修改意见**；按钮"修改并重新提交"（B13，新版本） |
| `Published` | 已报人数 / 容量、剩余名额、二维码、参与者名单、"提出修改"、"取消活动" |
| `Cancelled` | 显示取消原因，二维码改为"已取消"提示；已有报名记录保留 |

- **二维码**（B14）：只在**审核通过后**出现，内容是 `{站点地址}/register/{token}`。同时给出文字链接（"Registration link"）作为无障碍替代和复制用。
- **报名统计**（B20）：已报人数、容量、剩余名额，三项一起显示；报名成功后刷新应看到更新。
- **参与者名单**（来自 US07 / persona，backlog 里只写了人数）：每人出现一次，显示**姓名 + 学号**（避免重名）。
- **待审核的修改**：如果有未决的修改（B26），显示"修改正在审核中，学生目前看到的仍是已批准的版本"。

### 3.4 提出修改（已发布活动，B26–B30）
- 可修改：地点、**延后**报名截止、**增加**容量（US08）；backlog B27–B30 另外涉及**活动时间**的修改（US08 没写，以 backlog 为准）。
- 规则：新截止时间必须仍早于活动开始；容量必须是整数且不得小于当前已确认报名数（**[PROPOSED]**）。
- 提交后创建**新的审核项**；**审核期间，现有已批准的详情和名额限制继续有效**（B27）。
- 审核结果：
  - 通过 → 公开详情和学生的"已报名"详情更新；二维码**保持不变**，仍指向同一活动；地点变更会给已报名学生一条通知（B29）。
  - 退回（要求修改）→ 原已批准版本不变，组织者看到理由。
- **时间不变**时，已有报名自动保留（B28）。
- **时间变更获批**时（B30–B35）：已报名学生的名额变为 `reserved`，等学生确认；新开始时间前 24 小时仍未确认的自动取消。组织者页面应显示"已确认 / 待确认"人数。

### 3.5 取消活动（B23, B24）
- 点击"取消活动"→ 弹出确认对话框：显示活动名称，要求**输入原因**；用 `<dialog>`，焦点管理正确。
- 确认后：状态变 `cancelled`，**立即停止新报名**，**不需要再审核**；已报名学生看到取消和原因并收到通知；待发的提醒停止；已有二维码打开后显示取消信息，不能报名；报名记录保留。

---

## 4. 状态模型

**活动 (activity.status)**：

```
draft ──提交──▶ awaiting_review ──┬─通过──▶ published ──取消──▶ cancelled
                                  └─退回──▶ returned ──修改后重新提交──▶ awaiting_review
```
- `returned` 界面显示为 "Needs changes"。
- 已发布活动的修改是**新的审核项 (review item)**，不改变活动当前的 `published` 状态。

**审核项 (reviewItem)**：`kind`: `new` | `edit`；`status`: `pending` | `approved` | `returned`。

---

## 5. 组织者看到的数据 [PROPOSED]

```ts
type OrganiserActivity = {
  activityId: string; clubId: string; clubName: string
  title: string; description: string; location: string; requirements: string
  startsAt: string; registrationDeadline: string; capacity: number
  visibility: 'public'|'club_only'
  status: 'draft'|'awaiting_review'|'returned'|'published'|'cancelled'
  feedback?: { reason: string; decidedAt: string }     // returned 时
  cancelReason?: string
  qrToken?: string                                      // 仅 published
  confirmedCount: number; reservedCount: number; remaining: number
  pendingEdit?: { reviewId: string; proposed: Partial<ActivityFields>; submittedAt: string }
}

type Participant = { registrationId: string; studentName: string; universityId: string; status: 'confirmed'|'reserved' }
```
每个俱乐部有成员名单和最多 3 个组织者（B6）：`club_members(clubId, userId)`、`club_organisers(clubId, userId)`——**用 seed data**，账号管理不做。

---

## 6. API [PROPOSED]

前缀 `/api`；cookie session；所有接口服务端校验"角色 = organiser 且该活动属于我的俱乐部"。

| 方法 + 路径 | 作用 |
|---|---|
| `GET /organiser/clubs` | 我管理的俱乐部 |
| `GET /organiser/activities` | 我的活动列表 |
| `POST /organiser/activities` | 创建（`{ ...fields, submit: boolean }`；`submit=false` 存草稿） |
| `GET /organiser/activities/:id` | 活动管理详情（含状态、反馈、qrToken、统计） |
| `PUT /organiser/activities/:id` | 编辑草稿或 returned 的活动 |
| `POST /organiser/activities/:id/submit` | 提交 / 重新提交审核 |
| `POST /organiser/activities/:id/edits` | 对已发布活动提出修改（创建 `edit` 审核项） |
| `GET /organiser/activities/:id/participants` | 参与者名单（姓名 + 学号） |
| `POST /organiser/activities/:id/cancel` | 取消活动（`{ reason }`） |

**错误**：`VALIDATION_FAILED`（返回 `{ fields: { capacity: "必须是正整数" } }`，前端按字段显示）、`FORBIDDEN`（别的俱乐部）、`NOT_EDITABLE`（待审核时不可编辑）、`DEADLINE_AFTER_START`、`CAPACITY_BELOW_REGISTERED`。

---

## 7. mock 数据必须覆盖的情形

- 五种状态各一条（draft、awaiting_review、returned、published、cancelled）
- returned 带老师意见
- published 且已满；published 且无人报名
- 有待审核的修改（`pendingEdit`）
- 参与者名单：含重名但学号不同的两人；`reserved` 状态的参与者
- 表单：每个字段的校验失败、容量为 0 / 负数 / 小数、截止晚于开始
- 组织者管理多个俱乐部 / 只管理一个
- 别人俱乐部的 URL（应显示 403）
- 空列表、加载中、请求失败

---

## 8. 验收要点（摘自 US05–US09）

- 保存的草稿重新打开，内容完整。
- 缺失或无效的字段在字段旁提示，其他值保留。
- 提交后状态为 "Awaiting review"，没有二维码。
- 审核通过前没有公开报名页；通过后二维码出现。
- 名单、总数和容量放在一起显示；每位参与者一次，附学号。
- 修改获批前，学生仍看到旧详情；被退回后旧版本保留并显示原因。
- 取消需确认活动名称和原因；二维码改显示取消信息。
- 无障碍：表单 label 可见、错误关联字段（`aria-describedby`）、状态有文字、对话框可键盘操作。

---

## 9. 待确认 [OPEN]

1. ~~拒绝 (Reject)~~ **已决定**：冲突以 backlog 为准，只有通过 / 退回，**不做 Reject**。
2. **time change（B30–B35）** 工作量大（L）且要学生端、老师端配合，建议**最后做**，先做 B8–B10、B14、B20、B23，再做地点/截止/容量修改（B26–B28）。
3. 容量能否改小？（US08 只说"增加"）建议只允许增加，或不小于当前已确认数。
4. 活动是否有**结束时间** (`endsAt`)？backlog 只写"date and time"。
5. 参与者名单要不要导出（CSV）？Zihan persona 暗示有此需求，backlog 没写，先不做。
6. G17 stories 里的"发布俱乐部公告 (club announcement)"和"竞赛排名"**不在 backlog**，视为不做。
7. 管理员（US13–US15）**不在 backlog**；俱乐部成员、组织者名单用 seed data。
