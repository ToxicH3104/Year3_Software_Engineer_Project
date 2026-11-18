# 老师端规格 / Teacher-side spec

**来源**：`Project Backlog table.docx`（以 backlog 为准）+ `User Stories5.docx` / `User Story Group 15.docx` 里 US10–US12 的验收标准 + G17 里的 teacher stories。
**用途**：前端（React）先用 mock 数据实现；后端按此反推 API 和 SQLite 表。
标 **[PROPOSED]** 是我们自己提议的命名和路径；标 **[OPEN]** 需要团队决定。

主要用户：Qian Zhou，负责几个俱乐部的审核，教学之余处理审核。**用电脑**。
需求：一个队列找到待办、信息完整可检查、每个决定都对应她审核的那个版本。

> 老师端是三个端里**功能最少**的：本质上是"一个列表 + 一个详情 + 两三个按钮"。但没有它，活动无法发布，整条"发布 → 审核 → 二维码 → 报名"链路就断了，所以必须有，做精简即可。

---

## 1. backlog 对照

| ID | 内容 | P | Size | 谁做 |
|---|---|---|---|---|
| B4 | 登录（学生/组织者/老师） | 1 | M | 共用 |
| B11 | 老师查看待审核的提交及完整详情 | 1 | M | 前端 + 后端 |
| B12 | 老师**通过**，或**退回并给出反馈** | 1 | M | 前端 + 后端 |
| B26 | 已发布活动的修改作为新审核项提交 | 1 | L | 老师端要能审核 `edit` 类型 |
| B27 | 修改获批前，保持现有已批准详情 | 1 | M | 后端 |
| B34 | **开始时间前不足 24 小时**，不得批准时间变更 | 1 | M | 后端判断，前端禁用并说明 |
| B30 | 时间变更获批后预留名额 | 1 | L | 后端（老师点"通过"触发） |

---

## 2. 页面与路由 [PROPOSED]

| 路由 | 页面 | 作用 | backlog |
|---|---|---|---|
| `/teacher/reviews` | ReviewQueue | 待审核队列 | B11 |
| `/teacher/reviews/:id` | ReviewDetail | 审核详情 + 决定 | B11, B12, B26, B34 |
| `/teacher/history` | ReviewHistory | 已做出的决定 | US10（backlog 未单列） |

老师登录后默认进入 `/teacher/reviews`。导航：Pending Reviews、History、退出。

---

## 3. 各页面内容与交互

### 3.1 ReviewQueue（B11）
- **只显示分配给这位老师的俱乐部**中，等待审核的提交。
- 每项：俱乐部、活动标题、**提交时间**、**报名截止时间**、类型标记（"新活动" / "修改"）。
- 排序：**最早提交**（默认）、**最近的报名截止时间**。
- 做出决定后，该项从队列消失，但保留在 History。
- 空状态："没有待审核的活动"。

### 3.2 ReviewDetail（B11, B12, B26）
**新活动（`kind = new`）**——同一页显示：
- 介绍、日期时间、地点、参加要求、人数上限、报名截止时间、所属俱乐部、可见范围（public / club_only）。

**重新提交（被退回后再提交）**——还要显示：
- 上一次的**老师意见**。
- **变更了哪些字段**（高亮），旧值 vs 新值（US12）。

**已发布活动的修改（`kind = edit`，B26）**——显示：
- 变更字段的**旧值（已批准）和新值（建议）**对照，例如地点旧 → 新。
- 提示："批准前，学生看到的仍是旧版本"（B27）。

**决定**（B12）：

| 按钮 | 要求 | 结果 |
|---|---|---|
| **Approve 通过** | 无 | 新活动：发布并生成二维码；修改：新值替换公开详情，二维码不变 |
| **Request changes / Return 退回** | **必须写理由**（文本框） | 状态变 `returned`（界面标 "Needs changes"），保持未发布；组织者看到反馈并可重新提交 |

- 理由为空时：错误显示在文本框旁，并有汇总提醒，**已写内容保留**。
- 只有被分配到该俱乐部的老师能做决定。
- **B34 规则**：如果是**时间变更**，且新开始时间距现在**不足 24 小时**，**"通过"按钮禁用**，并说明"距新的开始时间不足 24 小时，无法批准时间变更"，只允许退回。
- 每个决定都要记录：**审核人、时间、意见、被审核的版本**。

### 3.3 ReviewHistory
- 列出已做决定的审核项：俱乐部、标题、决定、时间、意见。
- 点进去只读查看当时审核的那个版本。
- 空状态："还没有审核记录"。

---

## 4. 状态模型

**审核项 (reviewItem)**

```
pending ──通过──▶ approved
        └─退回──▶ returned   （组织者修改后会产生新的 pending 版本）
```
- 每个版本都是独立的审核项，**旧版本和旧决定保留**，便于追溯每个阶段批准了什么内容。
- 对应的活动状态变化：
  - `kind=new` 通过 → 活动 `published`；退回 → 活动 `returned`
  - `kind=edit` 通过 → 新值生效，活动仍 `published`；退回 → 原已批准版本不变

---

## 5. 老师看到的数据 [PROPOSED]

```ts
type ReviewItem = {
  reviewId: string
  activityId: string
  clubId: string; clubName: string
  kind: 'new' | 'edit'
  version: number                      // 第几次提交
  submittedAt: string
  submittedBy: string                  // 组织者姓名
  status: 'pending' | 'approved' | 'returned'
  proposed: ActivityFields             // 本次提交的完整内容
  approved?: ActivityFields            // 当前已批准版本（kind=edit 时有）
  changedFields?: string[]             // 与上一版相比变化的字段
  previousFeedback?: { reason: string; decidedAt: string }
  isTimeChange: boolean                // 是否包含开始时间变更
  canApprove: boolean                  // 服务端算出：false 时附 blockReason
  blockReason?: 'TOO_CLOSE_TO_START'
  decision?: { reviewerId: string; reviewerName: string; decidedAt: string; decision: 'approved'|'returned'; reason?: string }
}

type ActivityFields = {
  title: string; description: string; location: string; requirements: string
  startsAt: string; registrationDeadline: string; capacity: number
  visibility: 'public' | 'club_only'
}
```
老师与俱乐部的分配关系 `teacher_clubs(teacherId, clubId)` 用 **seed data**（账号管理不做）。

---

## 6. API [PROPOSED]

前缀 `/api`；cookie session；服务端校验"角色 = teacher 且该审核项属于我被分配的俱乐部"。

| 方法 + 路径 | 作用 |
|---|---|
| `GET /teacher/reviews?sort=submitted\|deadline` | 待审核队列 |
| `GET /teacher/reviews/:id` | 审核详情（含变更对比、上次意见、`canApprove`） |
| `POST /teacher/reviews/:id/decision` | 提交决定：`{ "decision": "approve" \| "return", "reason": "..." }` |
| `GET /teacher/history` | 已做决定的审核项 |

**错误**：`FORBIDDEN`（不是你的俱乐部）、`REASON_REQUIRED`（退回没写理由）、`TOO_CLOSE_TO_START`（B34）、`ALREADY_DECIDED`（已被处理，防止重复提交）、`NOT_FOUND`。

**通过时后端要做的事**：
- 新活动：`status = published`，生成 `qrToken`。
- 修改：把 `proposed` 写入活动；时间不变则保留报名（B28）；时间变更则把已确认报名转为 `reserved`（B30）；给已报名学生创建通知（B29）。

---

## 7. mock 数据必须覆盖的情形

- 新活动提交（最常见）
- 被退回后重新提交（有上次意见 + 变更字段高亮）
- 已发布活动的地点修改（旧值 vs 新值）
- 已发布活动的**时间变更**，且**距新开始不足 24 小时**（`canApprove = false`）
- 队列里多条，用于测试两种排序（提交时间不同、截止时间不同）
- 空队列、空历史
- 提交退回但没写理由（错误提示）
- 重复点击通过（`ALREADY_DECIDED`）
- 别的俱乐部的审核项（403）
- 加载中、请求失败

---

## 8. 验收要点（摘自 US10–US12）

- 队列只显示分配给我的、待审核的提交；可按最早提交或最近截止排序。
- 决定后该项离开队列，但留在历史里。
- 审核页一次看到：介绍、日期时间、地点、要求、容量、截止时间。
- 退回必须写理由，且理由对组织者可见。
- 通过新活动即发布；退回则保持未发布。
- 每个决定保留：审核人、时间、意见、被审核的版本。
- 重新提交能看到变更字段和旧意见；修改在被批准前**不会**替换已批准版本。
- 无障碍：决定按钮是真正的 `<button>`；理由文本框有可见 label；错误关联字段；对比和队列可键盘操作。

---

## 9. 待确认 [OPEN]

1. ~~要不要有 Reject？~~ **已决定**：冲突以 backlog 为准，只有 **通过 + 退回**，不做 Reject。
2. **History 页**不在 backlog，只在 US10 提及。先做简单版，或放到最后。
3. 老师和俱乐部的**分配**怎么来？没有管理员功能，用 seed data 固定。
4. 退回后的状态叫 `returned`（"Needs changes"）：User Stories5 用 "Needs Changes"，Group 15 用 "Returned for changes"，统一用 **Needs changes** 作界面文字。
5. 这位老师是否可以审核同一俱乐部里**自己没被分配**的活动？按 US11：**不可以**。
