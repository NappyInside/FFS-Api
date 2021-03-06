package tv.zerator.ffs.api.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.PreparedStatementHandle;

import alexmog.apilib.dao.DAO;
import alexmog.apilib.managers.DaoManager.Dao;
import lombok.Data;
import tv.zerator.ffs.api.dao.beans.AccountBean;
import tv.zerator.ffs.api.dao.beans.AccountBean.BroadcasterType;
import tv.zerator.ffs.api.dao.beans.EventBean;

@Dao(database = "general")
public class EventsDao extends DAO<EventBean> {

	public EventsDao(BoneCPDataSource dataSource) {
		super(dataSource);
	}

	@Override
	public int insert(EventBean data) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("INSERT INTO events "
						+ "(name, description, status, reserved_to_affiliates, reserved_to_partners) VALUES "
						+ "(?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
			prep.setString(1, data.getName());
			prep.setString(2, data.getDescription());
			prep.setString(3, data.getStatus().name());
			prep.setBoolean(4, data.isReservedToAffiliates());
			prep.setBoolean(5, data.isReservedToPartners());
			prep.executeUpdate();
			try (ResultSet rs = prep.getGeneratedKeys()) {
				if (rs.next()) return rs.getInt(1);
				throw new SQLException("Cannot insert element.");
			}
		}
	}
	
	private EventBean constructEvent(ResultSet rs) throws SQLException {
		EventBean bean = new EventBean();
		bean.setCurrent(rs.getBoolean("is_current"));
		bean.setDescription(rs.getString("description"));
		bean.setId(rs.getInt("id"));
		bean.setName(rs.getString("name"));
		bean.setReservedToAffiliates(rs.getBoolean("reserved_to_affiliates"));
		bean.setReservedToPartners(rs.getBoolean("reserved_to_partners"));
		bean.setStatus(EventBean.Status.valueOf(rs.getString("status")));
		return bean;
	}
	
	public List<EventBean> getEvents(EventBean.Status status, int start, int end) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT * FROM events WHERE status = ? LIMIT ?, ?")) {
			prep.setString(1, status.name());
			prep.setInt(2, start);
			prep.setInt(3, end);
			try (ResultSet rs = prep.executeQuery()) {
				List<EventBean> ret = new ArrayList<>();
				while (rs.next()) ret.add(constructEvent(rs));
				return ret;
			}
		}
	}
	
	public List<EventBean> getEvents(int start, int end) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT * FROM events LIMIT ?, ?")) {
			prep.setInt(1, start);
			prep.setInt(2, end);
			try (ResultSet rs = prep.executeQuery()) {
				List<EventBean> ret = new ArrayList<>();
				while (rs.next()) ret.add(constructEvent(rs));
				return ret;
			}
		}
	}
	
	public EventBean getEvent(int id) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT * FROM events WHERE id = ?")) {
			prep.setInt(1, id);
			try (ResultSet rs = prep.executeQuery()) {
				if (!rs.next()) return null;
				return constructEvent(rs);
			}
		}
	}
	
	public EventBean getCurrent() throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT * FROM events WHERE is_current = 1")) {
			try (ResultSet rs = prep.executeQuery()) {
				if (!rs.next()) return null;
				return constructEvent(rs);
			}
		}
	}

	@Override
	public EventBean update(EventBean data) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("UPDATE events SET "
						+ "name = ?, description = ?, status = ?, reserved_to_affiliates = ?, "
						+ "reserved_to_partners = ?, is_current = ? WHERE id = ?")) {
			prep.setString(1, data.getName());
			prep.setString(2, data.getDescription());
			prep.setString(3, data.getStatus().name());
			prep.setBoolean(4, data.isReservedToAffiliates());
			prep.setBoolean(5, data.isReservedToPartners());
			prep.setBoolean(6, data.isCurrent());
			prep.setInt(7, data.getId());
			if (data.isCurrent()) {
				try (PreparedStatementHandle prep2 = (PreparedStatementHandle) conn.prepareStatement("UPDATE events SET "
						+ "is_current = 0 WHERE is_current = 1")) {
					prep2.executeUpdate();
				}
			}
			prep.executeUpdate();
		}
		return data;
	}
	
	public List<Integer> getRounds(int eventId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT round_id FROM event_rounds WHERE event_id = ?")) {
			prep.setInt(1, eventId);
			try (ResultSet rs = prep.executeQuery()) {
				List<Integer> ret = new ArrayList<>();
				while (rs.next()) ret.add(rs.getInt("round_id"));
				return ret;
			}
		}
	}
	
	public boolean roundExists(int eventId, int roundId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT round_id FROM event_rounds WHERE event_id = ? AND round_id = ?")) {
			prep.setInt(1, eventId);
			prep.setInt(2, roundId);
			try (ResultSet rs = prep.executeQuery()) {
				return rs.next();
			}
		}
	}
	
	public int addRound(int eventId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("INSERT INTO event_rounds "
						+ "(event_id) VALUES "
						+ "(?)", Statement.RETURN_GENERATED_KEYS)) {
			prep.setInt(1, eventId);
			prep.executeUpdate();
			try (ResultSet rs = prep.getGeneratedKeys()) {
				if (rs.next()) return rs.getInt(1);
				throw new SQLException("Cannot insert element.");
			}
		}
	}
	
	public void deleteRound(int eventId, int roundId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("DELETE FROM event_rounds "
						+ "WHERE event_id = ? AND round_id = ?")) {
			prep.setInt(1, eventId);
			prep.setInt(2, roundId);
			prep.executeUpdate();
		}
	}
	
	public void addScore(int roundId, int accountId, int score) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("INSERT INTO round_scores "
						+ "(round_id, account_id, score) VALUES "
						+ "(?, ?, ?)")) {
			prep.setInt(1, roundId);
			prep.setInt(2, accountId);
			prep.setInt(3, score);
			prep.executeUpdate();
		}
	}
	
	public void updateScore(int roundId, int accountId, int score) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("UPDATE round_scores SET "
						+ "score = ? WHERE round_id = ? AND account_id = ?")) {
			prep.setInt(1, score);
			prep.setInt(2, roundId);
			prep.setInt(3, accountId);
			prep.executeUpdate();
		}
	}
	
	public @Data static class RoundUserScoreBean {
		private final String username, url, logo;
		private final int id, score;
	}
	
	public List<RoundUserScoreBean> getScores(int roundId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT s.score, a.username, a.url, a.twitch_id, a.logo FROM accounts a LEFT JOIN round_scores s ON s.account_id = a.twitch_id WHERE s.round_id = ?")) {
			prep.setInt(1, roundId);
			try (ResultSet rs = prep.executeQuery()) {
				List<RoundUserScoreBean> ret = new ArrayList<>();
				while (rs.next()) ret.add(new RoundUserScoreBean(rs.getString("a.username"), rs.getString("a.url"), rs.getString("a.logo"), rs.getInt("a.twitch_id"), rs.getInt("s.score")));
				return ret;
			}
		}
	}
	
	public Integer getScore(int roundId, int accountId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT score FROM round_scores WHERE round_id = ? AND account_id = ?")) {
			prep.setInt(1, roundId);
			prep.setInt(2, accountId);
			try (ResultSet rs = prep.executeQuery()) {
				if (rs.next()) return rs.getInt("score");
				return null;
			}
		}
	}
	
	public List<AccountBean> getUsers(int eventId, UserStatus status) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT a.twitch_id, a.username, a.email, a.views, a.followers, a.broadcaster_type, a.url, a.grade, a.logo "
						+ " FROM accounts a LEFT JOIN account_event_status s ON s.account_id = a.twitch_id WHERE s.event_id = ? AND s.status = ?")) {
			prep.setInt(1, eventId);
			prep.setString(2, status.name());
			try (ResultSet rs = prep.executeQuery()) {
				List<AccountBean> ret = new ArrayList<>();
				while (rs.next()) {
					AccountBean bean = new AccountBean();
					bean.setTwitchId(rs.getInt("a.twitch_id"));
					bean.setUsername(rs.getString("a.username"));
					bean.setEmail(rs.getString("a.email"));
					bean.setViews(rs.getInt("a.views"));
					bean.setFollowers(rs.getInt("a.followers"));
					bean.setBroadcasterType(BroadcasterType.valueOf(rs.getString("a.broadcaster_type")));
					bean.setUrl(rs.getString("a.url"));
					bean.setGrade(rs.getInt("a.grade"));
					bean.setLogo(rs.getString("a.logo"));
					ret.add(bean);
				}
				return ret;
			}
		}
	}
	
	public List<AccountBean> getUsers(int eventId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT a.twitch_id, a.username, a.email, a.views, a.followers, a.broadcaster_type, a.url, a.grade, a.logo "
						+ " FROM accounts a LEFT JOIN account_event_status s ON s.account_id = a.twitch_id WHERE s.event_id = ?")) {
			prep.setInt(1, eventId);
			try (ResultSet rs = prep.executeQuery()) {
				List<AccountBean> ret = new ArrayList<>();
				while (rs.next()) {
					AccountBean bean = new AccountBean();
					bean.setTwitchId(rs.getInt("a.twitch_id"));
					bean.setUsername(rs.getString("a.username"));
					bean.setEmail(rs.getString("a.email"));
					bean.setViews(rs.getInt("a.views"));
					bean.setFollowers(rs.getInt("a.followers"));
					bean.setBroadcasterType(BroadcasterType.valueOf(rs.getString("a.broadcaster_type")));
					bean.setUrl(rs.getString("a.url"));
					bean.setGrade(rs.getInt("a.grade"));
					bean.setLogo(rs.getString("a.logo"));
					ret.add(bean);
				}
				return ret;
			}
		}
	}

	public void delete(int eventId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("DELETE FROM events WHERE id = ?")) {
			prep.setInt(1, eventId);
			prep.executeUpdate();
		}
	}
	
	public enum UserStatus {
		VALIDATED,
		AWAITING_FOR_EMAIL_VALIDATION,
		AWAITING_FOR_ADMIN_VALIDATION,
		REFUSED
	}

	public void registerUser(int eventId, int accountId, UserStatus status, String emailActivationKey) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("INSERT INTO account_event_status "
						+ "(account_id, event_id, status, email_activation_key) VALUES (?, ?, ?, ?)")) {
			prep.setInt(1, accountId);
			prep.setInt(2, eventId);
			prep.setString(3, status.name());
			prep.setString(4, emailActivationKey);
			prep.executeUpdate();
		}
	}
	
	public void removeUser(int eventId, int accountId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("DELETE FROM account_event_status WHERE account_id = ? AND event_id = ?")) {
			prep.setInt(1, accountId);
			prep.setInt(2, eventId);
			prep.executeUpdate();
		}
	}
	
	public void updateUser(int eventId, int accountId, UserStatus status) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("UPDATE account_event_status SET status = ? WHERE account_id = ? AND event_id = ?")) {
			prep.setString(1, status.name());
			prep.setInt(2, accountId);
			prep.setInt(3, eventId);
			prep.executeUpdate();
		}
	}
	
	public class AccountStatusBean extends AccountBean {
		public UserStatus status;
	}
	
	public AccountStatusBean getRegistered(int eventId, int accountId) throws SQLException {
		try (Connection conn = mDataSource.getConnection();
				PreparedStatementHandle prep = (PreparedStatementHandle) conn.prepareStatement("SELECT a.twitch_id, a.username, a.email, a.views, a.followers, a.broadcaster_type, a.url, a.grade, s.status, a.logo "
						+ " FROM accounts a LEFT JOIN account_event_status s ON s.account_id = a.twitch_id WHERE s.event_id = ? AND a.twitch_id = ?")) {
			prep.setInt(1, eventId);
			prep.setInt(2, accountId);
			try (ResultSet rs = prep.executeQuery()) {
				if (rs.next()) {
					AccountStatusBean bean = new AccountStatusBean();
					bean.setTwitchId(rs.getInt("a.twitch_id"));
					bean.setUsername(rs.getString("a.username"));
					bean.setEmail(rs.getString("a.email"));
					bean.setViews(rs.getInt("a.views"));
					bean.setFollowers(rs.getInt("a.followers"));
					bean.setBroadcasterType(BroadcasterType.valueOf(rs.getString("a.broadcaster_type")));
					bean.setUrl(rs.getString("a.url"));
					bean.setGrade(rs.getInt("a.grade"));
					bean.setLogo(rs.getString("a.logo"));
					bean.status = UserStatus.valueOf(rs.getString("s.status"));
					return bean;
				}
				return null;
			}
		}
	}
}
