package edu.ucar.unidata.wmotables.repository;

import java.util.List;
import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.dao.RecoverableDataAccessException;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;


/**
 * The TableDao implementation.  Persistence mechanism is a database.
 */

public class JdbcTableDao extends JdbcDaoSupport implements TableDao {

     private SimpleJdbcInsert insertActor;

    /**
     * Looks up and retrieves a table from the database using the table id.
     * 
     * @param tableId  The id of the table we are trying to locate (will be unique for each table). 
     * @return  The table represented as a Table object.   
     * @throws RecoverableDataAccessException  If unable to lookup table with the given table id. 
     */
    public Table lookupTable(int tableId) {
        String sql = "SELECT * FROM tables WHERE tableId = ?";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), tableId); 
        if (tables.isEmpty()) {
            throw new RecoverableDataAccessException("Unable to look up table. No table found in the database for tableId: " + new Integer(tableId).toString());
        }         
        return tables.get(0);
    }

    /**
     * Looks up and retrieves a table from the persistence mechanism using the md5 value.
     * 
     * @param md5  The md5 check sum of the table we are trying to locate (will be unique for each table). 
     * @return  The table represented as a Table object.   
     * @throws RecoverableDataAccessException  If unable to lookup table with the given table md5.
     */
    public Table lookupTable(String md5) {
        String sql = "SELECT * FROM tables WHERE md5 = ?";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), md5); 
        if (tables.isEmpty()) {
            throw new RecoverableDataAccessException("Unable to look up table. No table found in the database for md5: " + md5);
        }         
        return tables.get(0);
    }

    /**
     * Requests a List of ALL tables from the persistence mechanism.
     * 
     * @return  A List of tables.   
     */
    public List<Table> getTableList() {
        String sql = "SELECT * FROM tables ORDER BY dateCreated DESC";               
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper());
        return tables;
    }

    /**
     * Requests a List of tables owned by a particular user from the persistence mechanism.
     * 
     * @param userId  The id of the user what owns the tables.
     * @return  A List of tables.   
     */
    public List<Table> getTableList(int userId) {
        String sql = "SELECT * FROM tables WHERE userId = ? ORDER BY dateCreated DESC";               
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), userId);
        return tables;
    }

    /**
     * Queries the persistence mechanism and returns the number of tables.
     * 
     * @return  The total number of tables as an int.   
     */
    public int getTableCount() {
        String sql = "SELECT count(*) FROM tables";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper());
        return tables.size();
    }

    /**
     * Queries the persistence mechanism and returns the number of tables owned by a user.
     * 
     * @param userId  The id of the user that owns the tables.
     * @return  The total number of tables as an int.  
     */
    public int getTableCount(int userId) {
        String sql = "SELECT count(*) FROM tables WHERE userId = ?";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), userId);
        return tables.size();
    }

    /**
     * Toggles the table's visiblity attribute to in the persistence mechanism.
     * 
     * @param tableId  The ID of the table in the persistence mechanism. 
     * @throws RecoverableDataAccessException  If unable to find the table to toggle. 
     */
    public void toggleTableVisibility(Table table) {
        String sql = "UPDATE tables SET visibility = ? WHERE tableId = ?";
        int rowsAffected = getJdbcTemplate().update(sql, new Object[] {
            // order matters here
            table.getVisibility(),
            table.getTableId()
        });
        if (rowsAffected <= 0) {
            throw new RecoverableDataAccessException("Unable to toggle table visibility. No entry found in the database for table: " + table.toString());
        }
    }

    /**
     * Creates a new table.
     * 
     * @param table  The table to be created.  
     * @throws RecoverableDataAccessException  If the table we are trying to create already exists.
     */
    public void createTable(Table table) {        
        String sql = "SELECT * FROM tables WHERE md5 = ?";
        List<Table> tables = getJdbcTemplate().query(sql, new TableMapper(), table.getMd5());        
        if (!tables.isEmpty()) {
            throw new RecoverableDataAccessException("Table already exists: " + table.toString());
        } else {
            this.insertActor = new SimpleJdbcInsert(getDataSource()).withTableName("tables").usingGeneratedKeyColumns("tableId");
            SqlParameterSource parameters = new BeanPropertySqlParameterSource(table);
            Number newTableId = insertActor.executeAndReturnKey(parameters);
            table.setTableId(newTableId.intValue());
        }                
    }

    /**
     * Saves changes made to an existing table. 
     * 
     * @param table   The existing table with changes that needs to be saved. 
     * @throws RecoverableDataAccessException  If unable to find the table to update. 
     */
    public void updateTable(Table table) {
        String sql = "UPDATE tables SET title = ?, description = ?, version = ?, dateModified = ? WHERE tableId = ?";
        int rowsAffected = getJdbcTemplate().update(sql, new Object[] {
            // order matters here
            table.getTitle(),
            table.getDescription(), 
            table.getVersion(), 
            table.getDateModified(),
            table.getTableId()
        });
        if (rowsAffected <= 0) {
            throw new RecoverableDataAccessException("Unable to update table.  No entry found in the database for table: " + table.toString());
        }
    } 


    /***
     * Maps each row of the ResultSet to a Table object.
     */
    private static class TableMapper implements RowMapper<Table> {
        /**
         * Maps each row of data in the ResultSet to the Table object.
         * 
         * @param rs  The ResultSet to be mapped.
         * @param rowNum  The number of the current row.
         * @return  The populated Table object.
         * @throws SQLException  If a SQLException is encountered getting column values.
         */
        public Table mapRow(ResultSet rs, int rowNum) throws SQLException {
            Table table = new Table();
            table.setTableId(rs.getInt("tableId"));
            table.setTitle(rs.getString("title"));
            table.setDescription(rs.getString("description"));
            table.setOriginalName(rs.getString("originalName"));
            table.setVersion(rs.getString("version"));
            table.setMd5(rs.getString("md5"));
            table.setUserId(rs.getInt("userId"));
            table.setVisibility(rs.getInt("visibility"));
            table.setDateCreated(rs.getTimestamp("dateCreated"));
            table.setDateModified(rs.getTimestamp("dateModified"));
            return table;
        }
    }


}
