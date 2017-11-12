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

package com.novell.ldapchai.provider;

import com.novell.ldapchai.util.StringHelper;

import javax.net.ssl.X509TrustManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * LDAP Chai API configuration settings.  This class represents the primary means
 * of controling Chai behavior.  Instances of {@code ChaiConfiguration} are semi-mutable.
 * Once instantiated, the setters may be called to modify the instance.  However, once {@link #lock()} is
 * called, all setters will throw an {@link IllegalStateException}.
 * <p>
 * When a {@code ChaiConfiguration} instance is used to configure a new ChaiProvider
 * instance, it is automatically locked.  Thus, a {@link ChaiProvider}'s configuration can not be modifed
 * once it is used to create a {@link ChaiProvider}.
 * <p>
 * This class is <i>cloneable</i> and clones are created in an unlocked state.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting
 */
public class ChaiConfiguration implements Serializable
{
    // comma <or> space (regex)
    public static final String LDAP_URL_SEPARATOR_REGEX_PATTERN = ",| ";

    private static final Properties DEFAULT_SETTINGS = new Properties();

    private static final long serialVersionUID = 1L;

    private Serializable implementationConfiguration;
    private transient volatile boolean locked;
    private Properties settings = new Properties( DEFAULT_SETTINGS );
    private transient X509TrustManager[] trustManager = null;

    static
    {
        for ( final ChaiSetting s : ChaiSetting.values() )
        {
            DEFAULT_SETTINGS.put( s.getKey(), s.getDefaultValue() );
        }
    }

    /**
     * Get a properties containing the default settings used by a newly constructed {@code ChaiConfiguration}.
     *
     * @return The default settings.
     */
    public static Properties getDefaultSettings()
    {
        final Properties propCopy = new Properties();
        propCopy.putAll( DEFAULT_SETTINGS );
        return propCopy;
    }

    /**
     * Construct a default {@code ChaiConfiguration}
     */
    public ChaiConfiguration()
    {
    }

    /**
     * Construct a default {@code ChaiConfiguration}
     *
     * @param bindDN       ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
     * @param bindPassword password for the bind DN.
     * @param ldapURLs     an ordered list fo ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
     */
    public ChaiConfiguration( final List<String> ldapURLs, final String bindDN, final String bindPassword )
    {
        this.setSetting( ChaiSetting.BIND_PASSWORD, bindPassword );
        this.setSetting( ChaiSetting.BIND_DN, bindDN );

        {
            final StringBuilder sb = new StringBuilder();
            for ( final String s : ldapURLs )
            {
                sb.append( s );
                sb.append( "," );
            }
            this.setSetting( ChaiSetting.BIND_URLS, sb.toString() );
        }
    }

    /**
     * Construct a new configuration based on the input configuration settings, including the bind DN, password and ldap URLs.  The
     * new instance will be unlocked, regardless of the lock status of the input configuratio.
     */
    public ChaiConfiguration( final ChaiConfiguration existingConfiguration )
    {
        final Properties newSettings = new Properties();
        for ( final Enumeration keyEnum = existingConfiguration.settings.propertyNames(); keyEnum.hasMoreElements(); )
        {
            final String keyName = ( String ) keyEnum.nextElement();
            newSettings.setProperty( keyName, existingConfiguration.settings.getProperty( keyName ) );
        }
        settings = newSettings;

        trustManager = existingConfiguration.trustManager;
        implementationConfiguration = existingConfiguration.implementationConfiguration;
        locked = false;
    }


    /**
     * Set a single settings.  Each setting is avalable in the {@link ChaiSetting} enumeration.
     *
     * @param setting the setting to set
     * @param value   the value to set
     * @return this instance of the {@link ChaiConfiguration} to facilitate chaining
     * @throws IllegalArgumentException if the value is not syntactically correct
     * @see ChaiSetting#validateValue(String)
     */
    public ChaiConfiguration setSetting( final ChaiSetting setting, final String value )
    {
        checkLock();
        setting.validateValue( value );
        this.settings.setProperty( setting.getKey(), value == null ? setting.getDefaultValue() : value );
        return this;
    }


    private void checkLock()
    {
        if ( locked )
        {
            throw new IllegalStateException( "configuration locked" );
        }
    }

    /**
     * Construct a default {@code ChaiConfiguration}
     *
     * @param bindDN       ldap bind DN, in ldap fully qualified syntax.  Also used as the DN of the returned ChaiUser.
     * @param bindPassword password for the bind DN.
     * @param ldapURL      ldap server and port in url format, example: <i>ldap://127.0.0.1:389</i>
     */
    public ChaiConfiguration( final String ldapURL, final String bindDN, final String bindPassword )
    {
        this.setSetting( ChaiSetting.BIND_PASSWORD, bindPassword );
        this.setSetting( ChaiSetting.BIND_DN, bindDN );
        this.setSetting( ChaiSetting.BIND_URLS, ldapURL );
    }

    /**
     * Return the current implementation configuration object.
     *
     * @return current implementation configuration object.
     */
    public Object getImplementationConfiguration()
    {
        return implementationConfiguration;
    }

    /**
     * Indicates the lock status of this {@code ChaiConfiguration}.
     *
     * @return true if this ChaiConfiguration is locked
     */
    public boolean isLocked()
    {
        return locked;
    }

    /**
     * Returns a string value suitable for debugging.  Sensitive values such as passwords are
     * not included.
     *
     * @return a string value suitable for debugging
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( ChaiConfiguration.class.getSimpleName() );
        sb.append( ": " );
        sb.append( "locked=" ).append( locked );
        sb.append( " settings: {" );

        for ( final ChaiSetting s : ChaiSetting.values() )
        {
            sb.append( s.getKey() );
            sb.append( "=" );
            if ( s.isVisible() )
            {
                sb.append( getSetting( s ) );
            }
            else
            {
                sb.append( "**stripped**" );
            }
            sb.append( ", " );
        }

        //remove the last ", " from the buffer
        sb = new StringBuilder( sb.toString().replaceAll( ", $", "" ) );

        sb.append( "}" );

        return sb.toString();
    }

    /**
     * Get an individual setting value
     *
     * @param setting the setting to return
     * @return the value or the default value if no value exists.
     */
    public String getSetting( final ChaiSetting setting )
    {
        return settings.getProperty( setting.getKey() );
    }

    /**
     * Get an individual setting value and test it as a boolean
     *
     * @param setting the setting to return
     * @return the value or the default value if no value exists.
     */
    public boolean getBooleanSetting( final ChaiSetting setting )
    {
        final String settingValue = getSetting( setting );
        return StringHelper.convertStrToBoolean( settingValue );
    }


    /**
     * Returns an immutable list of the ldap URLs.
     *
     * @return an immutable list of the ldapURLS.
     */
    public List<String> bindURLsAsList()
    {
        final List<String> results = new ArrayList<>();
        results.addAll( Arrays.asList( getSetting( ChaiSetting.BIND_URLS ).split( LDAP_URL_SEPARATOR_REGEX_PATTERN ) ) );
        return Collections.unmodifiableList( results );
    }

    String getBindPassword()
    {
        return settings.getProperty( ChaiSetting.BIND_PASSWORD.getKey() );
    }

    int getIntSetting( final ChaiSetting name )
    {
        try
        {
            return Integer.parseInt( getSetting( name ) );
        }
        catch ( Exception e )
        {
            // doesnt matter, we're throwing anyway.
        }
        throw new IllegalArgumentException( "misconfigured value; " + name + " should be Integer, but is not" );
    }

    /**
     * Get the current settings of the {@code ChaiProvider}.
     *
     * @return a copy of the settings for this ChaiConfiguration
     */
    public Properties getSettings()
    {
        // make a defensive copy
        final Properties propCopy = new Properties();
        propCopy.putAll( settings );
        return propCopy;
    }

    /**
     * Get the current settings of the {@code ChaiProvider}.
     *
     * @return a copy of the settings for this ChaiConfiguration
     */
    public X509TrustManager[] getTrustManager()
    {
        return trustManager == null ? null : Arrays.copyOf( trustManager, trustManager.length );
    }

    /**
     * Lock this {@code ChaiConfiguration}.  Once locked, all of the setter methods will throw an {@link IllegalStateException}.
     * In order to be locked, both an implementation class and implementation configuration must be set.
     */
    public void lock()
    {
        if ( getSetting( ChaiSetting.PROVIDER_IMPLEMENTATION ).length() < 1 )
        {
            throw new IllegalStateException( "implementation class is required to lock configuration" );
        }
        locked = true;
    }

    /**
     * Set an object to be used for the {@link ChaiProvider} implementation to be used for its configuration.  Depending
     * on the implementation, this could be any type of object such as a Properties, Map, or even an implementation specific object.
     * <p>
     * When used with the default provider, {@code JNDIProviderImpl}, this object must be a {@link java.util.Hashtable} environment as specified by the
     * {@link javax.naming.ldap.InitialLdapContext}.
     *
     * @param implementationConfiguration an object suitable to be used as a configuration for whatever {@code ChaiProvider} implementation is to be used.
     * @return this instance of the {@link ChaiConfiguration} to facilitate chaining
     */
    public ChaiConfiguration setImplementationConfiguration( final Serializable implementationConfiguration )
    {
        checkLock();
        this.implementationConfiguration = implementationConfiguration;
        return this;
    }

    /**
     * Add a TrustManager to be used when connecting to ssl ldap servers.
     *
     * @param trustManager A serializable trustmanager to be used for connecting to ldap servers.
     * @return this instance of the {@link ChaiConfiguration} to facilitate chaining
     */
    public ChaiConfiguration setTrustManager( final X509TrustManager[] trustManager )
    {
        checkLock();
        this.trustManager = trustManager == null ? null : Arrays.copyOf( trustManager, trustManager.length );
        return this;
    }

    /**
     * Set the settings in the {@code ChaiConfiguration}.  Each setting key is available as a constant publicly defined by
     * ChaiConfiguration.   The default settings are available in {@link #getDefaultSettings()}.
     *
     * @param settings a Properties containing settings to be used by the provider.  If a setting is missing in the
     *                 supplied Properties, the current setting will be unchanged.
     */
    public void setSettings( final Properties settings )
    {
        checkLock();
        final ChaiConfiguration tempConfig = new ChaiConfiguration();
        for ( final Object key : settings.keySet() )
        {
            final ChaiSetting setting = ChaiSetting.forKey( key.toString() );
            if ( setting != null )
            {
                tempConfig.setSetting( setting, settings.getProperty( key.toString() ) );
            }
        }

        final Properties newProps = new Properties();
        newProps.putAll( this.settings );
        newProps.putAll( tempConfig.settings );
        this.settings = newProps;
    }
}
