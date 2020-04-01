import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ChangeDueDate {

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub
		
		System.out.println("...starting");
		String dburl = "IP ADDRESS TO DATABASE";
		
		String myDriver = "com.mysql.jdbc.Driver";
	    String myUrl = "jdbc:mysql://"+ dburl + ":3306/ole";
	    Class.forName(myDriver);
	    Connection conn = DriverManager.getConnection(myUrl, "YOUR OLE USERID", "YOUR OLE PASSWORD");
	    
	    /*
	     * I RAN THIS SEVERAL TIMES...TRYING TO MAKE SURE NO LOAN WERE DUE BEFORE 8/31...AND NO NOTICES SEND BEFORE 
	     * LATE AUGUST AS WELL
	     * SO I RAN THIS SCRIPT SEVERAL TIMES
	     * ANYTHING WITH A DUE DATE BEFORE 4/1 ADD 180 DAYS TO DUE DATE AND NOTICES
	     * ANYTHING WITH A DUE DATE BEOFRE 5/1 ADD 150 DAYS TO DUE DATE AND NOTICES
	     * ANYTHING WITH A DUE DATE BEFORE 6/1 ADD 120 DAYS TO DUE DATE AND NOTICES
	     * ANYTHING WITH A DUE DATE BEOFRE 7/1 ADD 90   DAYS TO DUE DATE AND NOTICES
	     * ANYTHING WITH A DUE DATE BEFORE 8/1 ADD 60 DAYS TO DUE DATE AND NOTICES
	     * ANTHING WITH A DUE DATE BEFORE 9/1 ADD 30 DAYS TO THE DUE DATE AND NOTICES
	     * 
	     */
	    
	    StringBuffer buffer = new StringBuffer();
	    buffer.append(" select * from ole_dlvr_loan_t ");
	    buffer.append("	join ole_ptrn_t on ole_dlvr_loan_t.ole_ptrn_id = ole_ptrn_t.ole_ptrn_id");
	    buffer.append("	JOIN ole_dlvr_borr_typ_t on ole_dlvr_borr_typ_t.dlvr_borr_typ_id = ole_ptrn_t.borr_typ");
	    buffer.append("	join ole_ds_item_t on ole_ds_item_t.barcode = ole_dlvr_loan_t.itm_id");
	    buffer.append("	join ole_dlvr_item_avail_stat_t on ole_dlvr_item_avail_stat_t.item_avail_stat_id = ole_ds_item_t.item_status_id ");
	    buffer.append("	join ole_cat_itm_typ_t on ole_cat_itm_typ_t.itm_typ_cd_id = ole_ds_item_t.item_type_id");
	    //**THIS IS WHERE I CHANGED THE DATE
	    buffer.append("	where curr_due_dt_time < '2020-08-01'");
	    buffer.append("	and ole_dlvr_item_avail_stat_t.item_avail_stat_cd = 'LOANED' ");
	    //buffer.append("	and ITM_TYP_CD != 'DMS_LAPTOP'");
	    System.out.println(buffer.toString());
	    Statement st = conn.createStatement();	
	    Statement updateLoanStatement = conn.createStatement();
	    Statement updateNoticeStatement = conn.createStatement();
	    Statement updateItemStatement = conn.createStatement();
	      //System.out.println(query);
	      ResultSet rs = st.executeQuery(buffer.toString());
	      while (rs.next()) {
	    	  String loanId = rs.getString("loan_tran_id");
	    	  String barcode = rs.getString("itm_id");
	    	  
	    	  //A FEW I WANTED TO IGNORE
	    	  if (barcode.equalsIgnoreCase("39151003588759")) continue;
	    	  if (barcode.equalsIgnoreCase("39151008336089")) continue;
	    	  if (barcode.equalsIgnoreCase("39151008201564")) continue;
	    	  
	    	  System.out.print(rs.getString("CURR_DUE_DT_TIME") + " --- ");
	    	  System.out.print(loanId);
	    	  System.out.println("---" + barcode);
	    	  //THIS IS WHERE I CHANGED THE NUMBER OF DAYS ADDED TO LOANS AND NOTICES
	    	  //(THERE IS A DUE DATE IN THE ITEM TABLE YOU HAVE TO CHANGE TOO!  :)
	    	  String updateLoanQuery = "UPDATE ole_dlvr_loan_t SET CURR_DUE_DT_TIME = DATE_ADD(CURR_DUE_DT_TIME , INTERVAL 60 DAY) WHERE loan_tran_id = " + loanId;
	    	  String updateNoticeQuery = "UPDATE ole_dlvr_loan_notice_t  SET NTC_TO_SND_DT = DATE_ADD(NTC_TO_SND_DT,INTERVAL 60 DAY) WHERE LOAN_ID = " + loanId;
	    	  String itemUpdateQuery = "UPDATE ole_ds_item_t set due_date_time = DATE_ADD(DUE_DATE_TIME ,INTERVAL 60 DAY)  where barcode = '" + barcode + "'";
	    	  try {
	    		  updateLoanStatement.execute(updateLoanQuery);
	    	  }
	    	  catch(Exception e) {
	    		  System.out.println("DIDN'T UDATE LOAN:");
	    		  System.out.println(loanId);
	    		  System.out.println(e.getLocalizedMessage());
	    		  return;
	    	  }
	    	  
	    	  try {
	    		  updateNoticeStatement.execute(updateNoticeQuery);
	    	  }
	    	  catch(Exception e) {
	    		  System.out.println("DIDN'T UDATE NOTICES FOR LOAN:");
	    		  System.out.println(loanId);
	    		  System.out.println(e.getLocalizedMessage());
	    		  return;
	    	  }
	    	  
	    	  try {
	    		  updateItemStatement.execute(itemUpdateQuery);
	    	  }
	    	  catch(Exception e) {
	    		  System.out.println("DIDN'T UDATE ITEM FOR LOAN:");
	    		  System.out.println(itemUpdateQuery);
	    		  System.out.println(loanId);
	    		  System.out.println(e.getLocalizedMessage());
	    		  return;
	    	  }
	    	  
	    	  
	      }
		
		conn.close();
		System.out.println("done...");

	}

}
