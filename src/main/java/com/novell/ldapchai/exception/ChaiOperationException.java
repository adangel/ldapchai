/*
 * LDAP Chai API
 * Copyright (c) 2006-2010 Novell, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.novell.ldapchai.exception;

/**
 * General exception for all types of LDAP errors generated by Chai.
 * <p>
 * The underlying message and/or error code is inferred from responses generated by the
 * ldap server or underlying API such as JNDI.
 *
 * @author Jason D. Rivard
 * @see ChaiError
 */
public class ChaiOperationException extends ChaiException
{

    public static ChaiOperationException forErrorMessage( final String errorMessage )
    {
        return new ChaiOperationException( errorMessage, ChaiErrors.getErrorForMessage( errorMessage ) );
    }

    public ChaiOperationException( final String message, final ChaiError errorCode )
    {
        super( message, errorCode );
    }

    public ChaiOperationException( final String message, final ChaiError errorCode, final boolean permenant, final boolean authentication )
    {
        super( message, errorCode, permenant, authentication );
    }

}

