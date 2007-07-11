package org.owasp.webgoat.lessons.SQLInjection;

import java.util.ArrayList;
import java.util.List;
import org.apache.ecs.ElementContainer;
import org.owasp.webgoat.lessons.Category;
import org.owasp.webgoat.lessons.GoatHillsFinancial.DeleteProfile;
import org.owasp.webgoat.lessons.GoatHillsFinancial.EditProfile;
import org.owasp.webgoat.lessons.GoatHillsFinancial.FindProfile;
import org.owasp.webgoat.lessons.GoatHillsFinancial.GoatHillsFinancial;
import org.owasp.webgoat.lessons.GoatHillsFinancial.LessonAction;
import org.owasp.webgoat.lessons.GoatHillsFinancial.Logout;
import org.owasp.webgoat.lessons.GoatHillsFinancial.SearchStaff;
import org.owasp.webgoat.lessons.GoatHillsFinancial.UpdateProfile;
import org.owasp.webgoat.session.ParameterNotFoundException;
import org.owasp.webgoat.session.UnauthenticatedException;
import org.owasp.webgoat.session.UnauthorizedException;
import org.owasp.webgoat.session.ValidationException;
import org.owasp.webgoat.session.WebSession;

/*******************************************************************************
 * 
 * 
 * This file is part of WebGoat, an Open Web Application Security Project
 * utility. For details, please see http://www.owasp.org/
 * 
 * Copyright (c) 2002 - 2007 Bruce Mayhew
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 * Getting Source ==============
 * 
 * Source for this application is maintained at code.google.com, a repository
 * for free software projects.
 * 
 * For details, please see http://code.google.com/p/webgoat/
 */
public class SQLInjection extends GoatHillsFinancial
{
    private final static Integer DEFAULT_RANKING = new Integer(75);

    public final static int PRIZE_EMPLOYEE_ID = 112;

    public final static String PRIZE_EMPLOYEE_NAME = "Neville Bartholomew";

    public void registerActions(String className)
    {
	registerAction(new ListStaff(this, className, LISTSTAFF_ACTION));
	registerAction(new SearchStaff(this, className, SEARCHSTAFF_ACTION));
	registerAction(new ViewProfile(this, className, VIEWPROFILE_ACTION));
	registerAction(new EditProfile(this, className, EDITPROFILE_ACTION));
	registerAction(new EditProfile(this, className, CREATEPROFILE_ACTION));

	// These actions are special in that they chain to other actions.
	registerAction(new Login(this, className, LOGIN_ACTION,
		getAction(LISTSTAFF_ACTION)));
	registerAction(new Logout(this, className, LOGOUT_ACTION,
		getAction(LOGIN_ACTION)));
	registerAction(new FindProfile(this, className, FINDPROFILE_ACTION,
		getAction(VIEWPROFILE_ACTION)));
	registerAction(new UpdateProfile(this, className,
		UPDATEPROFILE_ACTION, getAction(VIEWPROFILE_ACTION)));
	registerAction(new DeleteProfile(this, className,
		DELETEPROFILE_ACTION, getAction(LISTSTAFF_ACTION)));
    }

    /**
     *  Gets the category attribute of the CrossSiteScripting object
     *
     * @return    The category value
     */
    public Category getDefaultCategory()
    {
	return Category.A6;
    }

    /**
     *  Gets the hints attribute of the DirectoryScreen object
     *
     * @return    The hints value
     */
    protected List<String> getHints(WebSession s)
    {
	List<String> hints = new ArrayList<String>();
	hints
		.add("The application is taking your input and inserting it at the end of a pre-formed SQL command.");
	hints
		.add("This is the code for the query being built and issued by WebGoat:<br><br> "
			+ "\"SELECT * FROM employee WHERE userid = \" + userId + \" and password = \" + password");
	hints
		.add("Compound SQL statements can be made by joining multiple tests with keywords like AND and OR.  "
			+ "Try appending a SQL statement that always resolves to true");

	// Stage 1
	hints
		.add("You may need to use WebScarab to remove a field length limit to fit your attack.");
	hints.add("Try entering a password of [ smith' OR '1' = '1 ].");

	// Stage 2
	hints
		.add("Many of WebGoat's database queries are already parameterized.  Search the project for PreparedStatement.");

	// Stage 3
	hints
		.add("Try entering a password of [ 101 OR 1=1 ORDER BY 'salary' ].");

	// Stage 4

	return hints;
    }

    @Override
	public int getStageCount() {
		return 4;
	}

    /**
     *  Gets the instructions attribute of the ParameterInjection object
     *
     * @return    The instructions value
     */
    public String getInstructions(WebSession s)
    {
	String instructions = "";

	if (!getLessonTracker(s).getCompleted())
	{
	    switch (getStage(s))
	    {
		case 1:
		    instructions = "Stage "
			    + getStage(s)
			    + ": Use String SQL Injection to bypass authentication. "
			    + "The goal here is to login as the user "
			    + PRIZE_EMPLOYEE_NAME
			    + ", who is in the Admin group.  "
			    + "You do not have the password, but the form is SQL injectable.";
		    break;
		case 2:
		    instructions = "Stage "
			    + getStage(s)
			    + ": Use a parameterized query.<br>"
			    + "A dynamic SQL query is not necessary for the login function to work.  Change login "
			    + "to use a parameterized query to protect against malicious SQL in the query parameters.";
		    break;
		case 3:
		    instructions = "Stage "
			    + getStage(s)
			    + ": Use Integer SQL Injection to bypass access control.<br>"
			    + "The goal here is to view the CEO's employee profile, again, even with data access "
			    + "control checks in place from a previous lesson.  "
			    + "As before, you do not have the password, but the form is SQL injectable.";
		    break;
		case 4:
		    instructions = "Stage "
			    + getStage(s)
			    + ": Use a parameterized query again.<br>"
			    + "Change the ViewProfile function to use a parameterized query to protect against "
			    + "malicious SQL in the numeric query parameter.";
		    break;
		default:
		    // Illegal stage value
		    break;
	    }
	}

	return instructions;
    }

    public void handleRequest(WebSession s)
    {
	if (s.getLessonSession(this) == null)
	    s.openLessonSession(this);

	String requestedActionName = null;
	try
	{
	    requestedActionName = s.getParser().getStringParameter("action");
	}
	catch (ParameterNotFoundException pnfe)
	{
	    // Let them eat login page.
	    requestedActionName = LOGIN_ACTION;
	}

	if (requestedActionName != null)
	{
	    try
	    {
		LessonAction action = getAction(requestedActionName);
		if (action != null)
		{
		    //System.out.println("CrossSiteScripting.handleRequest() dispatching to: " + action.getActionName());
		    if (!action.requiresAuthentication()
			    || action.isAuthenticated(s))
		    {
			action.handleRequest(s);
			//setCurrentAction(s, action.getNextPage(s));
		    }
		}
		else
		    setCurrentAction(s, ERROR_ACTION);
	    }
	    catch (ParameterNotFoundException pnfe)
	    {
		System.out.println("Missing parameter");
		pnfe.printStackTrace();
		setCurrentAction(s, ERROR_ACTION);
	    }
	    catch (ValidationException ve)
	    {
		System.out.println("Validation failed");
		ve.printStackTrace();
		setCurrentAction(s, ERROR_ACTION);
	    }
	    catch (UnauthenticatedException ue)
	    {
		s.setMessage("Login failed");
		System.out.println("Authentication failure");
		ue.printStackTrace();
	    }
	    catch (UnauthorizedException ue2)
	    {
		s.setMessage("You are not authorized to perform this function");
		System.out.println("Authorization failure");
		ue2.printStackTrace();
	    }
	    catch (Exception e)
	    {
		// All other errors send the user to the generic error page
		System.out.println("handleRequest() error");
		e.printStackTrace();
		setCurrentAction(s, ERROR_ACTION);
	    }
	}

	// All this does for this lesson is ensure that a non-null content exists.
	setContent(new ElementContainer());
    }

    protected Integer getDefaultRanking()
    {
	return DEFAULT_RANKING;
    }


    /**
     *  Gets the title attribute of the CrossSiteScripting object
     *
     * @return    The title value
     */
    public String getTitle()
    {
	return "LAB: SQL Injection";
    }
}